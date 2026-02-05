(ns lambda-lifters.paparascii.file-system
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [lambda-lifters.lambda-liftoff.io :as ll-io]
            [lambda-lifters.lambda-liftoff.zip-fetch :as zf]
            [lambda-lifters.paparascii.clean :as clean]
            [lambda-lifters.paparascii.file-patterns :as file-patterns]
            [lambda-lifters.paparascii.site :as site])
  (:import (java.io File IOException)
           (java.nio.file Path)))

(defn setup-target!
  "Create the TARGET directory structure"
  []
  (log/info "Setting up TARGET directory structure...")
  (dorun (->> site/target-dirs
              (map site/target-path)
              (map ll-io/ensure-directory))))

(defn copy-resources!
  "Copy all static resources to TARGET/public_html"
  []
  (log/info "Copying static resources...")
  (doseq [[src dst] site/resource-destinations
          :let [src-file (str @site/*site-dir "/resources/" src)
                dst-file (site/target-path dst)]]
    (ll-io/copy-file src-file dst-file))
  ;; Copy any other static assets if they exist
  (dorun (->> "resources"
              (io/file @site/*site-dir)
              .listFiles
              (filter File/.isFile)
              (remove #((set (keys site/resource-destinations)) (File/.getName %)))
              (map #(ll-io/copy-file % (site/public-html-file (.getName %)))))))

(defn- make-path-executable!
  "Make CGI scripts executable if they match configured patterns"
  [p f]
  (when (file-patterns/path-matches-any-glob? p (:cgi-bin-executable-patterns @site/*site-config))
    (log/info "Making executable:" p)
    (.setExecutable f true)))

(defn- copy-file-to-subdirectory! [subdir is-cgi? target-dir file]
  (let [relative-path (subs (.getPath file) (inc (count (.getPath subdir))))
        target-file (io/file target-dir relative-path)]
    (io/make-parents target-file)
    (io/copy file target-file)
    (when is-cgi? (make-path-executable! relative-path target-file))))

(defn- copy-assets-from-subdir! [subdir]
  (let [subdir-name (.getName subdir)
        is-cgi? (= subdir-name "cgi-bin")
        target-dir (io/file (site/public-html-path subdir-name))]
    (log/info "Copying" (str subdir-name "/"))
    (io/make-parents (io/file target-dir "dummy"))
    (dorun (->> subdir
                file-seq
                (filter File/.isFile)
                (map #(copy-file-to-subdirectory! subdir is-cgi? target-dir %))))))

(defn copy-assets!
  "Copy all assets from assets/ to TARGET/public_html/"
  []
  (log/info "Copying assets...")
  (when-let [assets-dir (site/existent-site-file "assets")]
    (dorun (->> assets-dir
                .listFiles
                (filter File/.isDirectory)
                (map copy-assets-from-subdir!)))))

(defn potential-bb-sources []
  (let [home (System/getenv "HOME")]
    ["/usr/local/bin/bb"
     "/usr/bin/bb"
     "/opt/homebrew/bin/bb"
     (str home "/.nix-profile/bin/bb")
     (str home "/bin/bb")]))

(defn copy-babashka!
  "Copy the current Babashka executable to TARGET/bin/"
  []
  (log/info "Copying Babashka executable...")
  (if-let [bb-path (some #(.exists (io/file %)) (potential-bb-sources))]
    (do
      (log/info "copy from" bb-path)
      (io/make-parents (site/target-path "bin" "bb"))
      (io/copy (io/file bb-path) (site/target-file "bin" "bb"))
      (.setExecutable (site/target-file "bin" "bb") true))
    (log/warn "Warning: Could not find Babashka executable to copy")))

(defn font-awesome-file-name-mapping [output-dir entry-name]
  (condp (fn [matcher nm] (matcher nm)) entry-name
    #(re-matches #"^.*fonts/(.*)" %) :>> #(io/file output-dir "fonts" (second %))
    #(re-matches #"^.*[^s]css/(.*)" %) :>> #(io/file output-dir "css" (second %))
    nil))

(defn install-standard-assets!
  "Installs the standard fonts, css and js required for AsciiDoc to work, notably with fontawesome
  From: https://docs.asciidoctor.org/asciidoctor/latest/html-backend/local-font-awesome/
  The first thing you’ll need to do is download a copy of Font Awesome.
  The HTML converter currently integrates with Font Awesome 4, so make sure you’re using that version."
  []
  (try
    (zf/fetch-and-unzip "https://fontawesome.com/v4/assets/font-awesome-4.7.0.zip"
                        :file-name-mapping #(font-awesome-file-name-mapping (site/public-html-path) %))
    (catch IOException ioe (log/error
                             "could not download fontawesome... due to IO (probably network issues) awesomeness will be degraded:"
                             (class ioe) ":"
                             (ex-message ioe)))))

(defn build-file-system! []
  ;; Clean and setup TARGET
  (log/info "Setup from scratch")
  (clean/clean-target!)
  (setup-target!)
  ;; Copy static resources
  (copy-resources!)
  ;; Copy assets (css, js, img, cgi-bin)
  (copy-assets!)
  (install-standard-assets!)
  ;; Copy Babashka executable (optional - could be useful for CGI scripts)
  (copy-babashka!))

(defn write-tag-index!
  "Generate an index page for a specific tag"
  [{:keys [slug html]}]
  (let [output-file (site/public-html-path "blog" "tags" (str slug ".html"))]
    (io/make-parents output-file)
    (spit output-file html)))

(defn write-index! [index]
  (spit (site/public-html-path "index.html") index))

(defn write-site-page!
  [[_k {:keys [html slug]}]]
  (let [file-name (str slug ".html")
        output-file (site/public-html-path file-name)]
    (log/info "processing" file-name)
    (io/make-parents output-file)
    (spit output-file html)))

(defn write-blog-post!
  [{:keys [html slug] :as post}]
  (log/info "writing post" (:slug post) slug)
  (let [output-file-name (str slug ".html")
        output-file (site/public-html-path "blog" output-file-name)]
    (log/info output-file-name)
    (io/make-parents output-file)
    (spit output-file html)))

(defn transpose-file [^Path source make-dest-path ^File f]
  (make-dest-path (.toFile (.relativize source (.toPath f)))))
