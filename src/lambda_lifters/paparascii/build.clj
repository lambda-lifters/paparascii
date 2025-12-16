(ns lambda-lifters.paparascii.build
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [hashp.preload]
            [lambda-lifters.lambda-liftoff.io :as ll-io]
            [lambda-lifters.lambda-liftoff.zip-fetch :as zf]
            [lambda-lifters.paparascii.adoc :as adoc]
            [lambda-lifters.paparascii.clean :as clean]
            [lambda-lifters.paparascii.prism-js-highlighter :as highlighter]
            [lambda-lifters.paparascii.site :as site]
            [lambda-lifters.paparascii.site-layout :as layout]
            [lambda-lifters.paparascii.util :as u]
            [selmer.parser :as selmer])
  (:import (java.io File)))

(defn copy-resources!
  "Copy all static resources to TARGET/public_html"
  []
  (log/info "Copying static resources...")
  (doseq [[src dst] site/resource-destinations]
    (ll-io/copy-file (str @site/*site-dir "/resources/" src) (site/target-path dst)))

  ;; Copy any other static assets if they exist
  (doseq [file (filter (every-pred File/.isFile
                                   (complement #((set (keys site/resource-destinations)) (File/.getName %))))
                       (.listFiles (io/file @site/*site-dir "resources")))]
    (ll-io/copy-file file (site/public-html-file (.getName file)))))

(defn copy-assets!
  "Copy all assets from assets/ to TARGET/public_html/"
  []
  (log/info "Copying assets...")
  (let [assets-dir (site/site-file "assets")]
    (when (.exists assets-dir)
      (doseq [subdir (filter File/.isDirectory (.listFiles assets-dir))]
        (let [subdir-name (.getName subdir)
              target-dir (io/file (site/public-html-path subdir-name))]
          (log/info "copying" (str subdir-name "/"))
          (io/make-parents (io/file target-dir "dummy"))
          (doseq [file (filter File/.isFile (file-seq subdir))]
            (let [relative-path (subs (.getPath file) (inc (count (.getPath subdir))))
                  target-file (io/file target-dir relative-path)]
              (io/make-parents target-file)
              (io/copy file target-file)
              ;; Make CGI scripts executable
              (when (= subdir-name "cgi-bin")
                (.setExecutable target-file true)))))))))

(defn copy-babashka!
  "Copy the current Babashka executable to TARGET/bin/"
  []
  (log/info "Copying Babashka executable...")
  (let [bb-path (first (filter #(.exists (io/file %))
                               ["/usr/local/bin/bb"
                                "/usr/bin/bb"
                                "/opt/homebrew/bin/bb"
                                (str (System/getenv "HOME") "/.nix-profile/bin/bb")
                                (str (System/getenv "HOME") "/bin/bb")]))]
    (if bb-path
      (do
        (log/info "copy from" bb-path)
        (io/make-parents (site/target-path "bin" "bb"))
        (io/copy (io/file bb-path) (site/target-file "bin" "bb"))
        (.setExecutable (site/target-file "bin" "bb") true))
      (log/warn "⚠ Warning: Could not find Babashka executable to copy"))))

(defn setup-target!
  "Create the TARGET directory structure"
  []
  (log/info "Setting up TARGET directory structure...")
  (doseq [dir site/target-dirs]
    (ll-io/ensure-directory (site/target-path dir))))

(defn site-page-html [content file]
  (let [content (selmer/render content @site/*site-config)
        header (adoc/parse-asciidoc-header content)]
    [header (layout/site-page-layout @site/*site-config header (adoc/asciidoc-to-html content file))]))

(defn process-site-page!
  "Process a single site page (about, contact, etc.)"
  [file]
  (let [filename (.getName (io/file file))
        basename (str/replace filename #"\.adoc$" "")
        file-name (str basename ".html")
        output-file (site/public-html-path file-name)]
    (log/info "processing " file-name)
    (io/make-parents output-file)
    (let [[header html] (site-page-html (slurp file) file)]
      (spit output-file html)
      header)))

(defn process-site-pages!
  "Process all pages in the site/ directory"
  [parallel?]
  (let [site-dir (site/site-file "site")]
    (when (.exists site-dir)
      (log/info (str "Processing site pages... parallel?=" parallel?))
      (let [ascii-doc-files (filter site/asciidoc-file-name? (.listFiles site-dir))]
        (log/info "AsciiDoc site pages")
        (doall (map #(process-site-page! (.getPath %)) ascii-doc-files))))))

(defn blog-post-html [content file]
  (let [page-meta (adoc/parse-asciidoc-header content)
        [html-content additional-css]
        (binding [highlighter/additional-header-css nil]
          [(adoc/asciidoc-to-html content file) highlighter/additional-header-css])]
    [(layout/blog-post-layout @site/*site-config page-meta html-content additional-css) page-meta]))

(defn process-blog-post!
  "Process a single blog post file"
  [file]
  (let [content (slurp file)
        filename (.getName (io/file file))
        basename (str/replace filename #"\.adoc$" "")
        output-file-name (str basename ".html")
        output-file (site/public-html-path "blog" output-file-name)]
    (log/info output-file-name)
    (io/make-parents output-file)
    (let [[output page-meta] (blog-post-html content file)]
      (spit output-file output)
      {:file basename :page-meta page-meta})))

(defn tag-index-html [tag posts]
  (let [sorted-posts (->> posts
                          (filter #(some #{tag} (get-in % [:page-meta :tags])))
                          (sort-by #(get-in % [:page-meta :date]))
                          reverse)]
    [(layout/tag-index-layout @site/*site-config tag sorted-posts) (count sorted-posts)]))

(defn generate-tag-index!
  "Generate an index page for a specific tag"
  [tag posts]
  (let [tag-slug (u/slugify tag)
        html-file-name (str tag-slug ".html")
        output-file (site/public-html-path "blog" "tags" html-file-name)
        [html n-tag-posts] (tag-index-html tag posts)]
    (log/info "generating tag index " (str "blog/tags/" html-file-name) "(" n-tag-posts "posts)")
    (io/make-parents output-file)
    (spit output-file html)))

(defn generate-all-tag-indexes!
  "Generate index pages for all tags"
  [posts]
  (let [all-tags (distinct (flatten (map #(get-in % [:page-meta :tags]) posts)))]
    (when-let [valid-tags (seq (filter some? all-tags))]
      (log/info "Generating tag indexes...")
      (doseq [tag valid-tags]
        (generate-tag-index! tag posts)))))

(defn generate-index!
  "Generate the index page with all blog posts"
  [posts]
  (log/info "Generating index.html")
  (spit (site/public-html-path "index.html")
        (layout/index-layout @site/*site-config posts)))

(defn font-awesome-file-name-mapping [output-dir entry-name]
  (condp #(%1 %2) entry-name
    (partial re-matches #"^.*fonts/(.*)") :>> #(io/file output-dir "fonts" (second %))
    (partial re-matches #"^.*[^s]css/(.*)") :>> #(io/file output-dir "css" (second %))
    nil))

(defn install-standard-assets!
  "Installs the standard fonts, css and js required for AsciiDoc to work, notably with fontawesome

  From: https://docs.asciidoctor.org/asciidoctor/latest/html-backend/local-font-awesome/
  The first thing you’ll need to do is download a copy of Font Awesome.
  The HTML converter currently integrates with Font Awesome 4, so make sure you’re using that version."
  []
  (zf/fetch-and-unzip "https://fontawesome.com/v4/assets/font-awesome-4.7.0.zip"
                      :file-name-mapping (partial font-awesome-file-name-mapping (site/public-html-path))))

(defn build! [& {:keys [parallel?] :or {parallel? true}}]
  (log/info "Building Static Blog")
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
  (copy-babashka!)

  (log/debug "realising AsciiDoctor")
  (deref adoc/*asciidoctor)

  ;; Process site pages (about, contact, etc.)
  (log/info "Processing site pages")
  (let [processed-pages (process-site-pages! parallel?)]
    ; TODO: this should inform the navbar
    (log/info "processed:" (map :title processed-pages)))
  ;; Process blog posts
  (log/info "Processing blog posts...")
  (let [blog-dir (io/file @site/*site-dir "blog")
        blog-files (filter site/asciidoc-file-name?
                           (or (seq (.listFiles blog-dir)) []))
        posts (doall (map #(process-blog-post! (.getPath %)) blog-files))]
    ;; Generate index
    (log/info "Generating index and tag-index pages...")
    (generate-index! posts)
    (generate-all-tag-indexes! posts)

    (log/info "
        Generated " (count posts) " blog posts
        Website ready in TARGET/public_html/
        To deploy:
          1. Copy TARGET/public_html/* to your web server
          2. Ensure .htaccess is copied (it may be hidden)
        To test locally:
          clojure -Tpaparascii serve")))

(comment
  ; Example REPL session if you need to debug a thing
  (System/setProperty "SITE_DIR" "../timb.net-site")
  (System/getProperty "SITE_DIR")
  (swap! site/*site-dir site/resolve-site-dir)
  @site/*site-dir
  @site/*site-config
  (build!)
  )
