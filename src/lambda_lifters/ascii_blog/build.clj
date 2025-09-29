(ns lambda-lifters.ascii-blog.build
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [hashp.preload]
            [lambda-lifters.ascii-blog.adoc :as adoc]
            [lambda-lifters.ascii-blog.clean :as clean]
            [lambda-lifters.ascii-blog.log :as log]
            [lambda-lifters.ascii-blog.prism-js-highlighter :as highlighter]
            [lambda-lifters.ascii-blog.site :as site]
            [lambda-lifters.ascii-blog.site-layout :as layout]
            [lambda-lifters.ascii-blog.util :as u])
  (:import (java.io File)))

(defn copy-resources!
  "Copy all static resources to TARGET/public_html"
  []
  (log/in-section
    "Copying static resources..."
    (doseq [[src dst] site/resource-destinations]
      (u/copy-file (str @site/*site-dir "/resources/" src) (site/target-path dst))))

  ;; Copy any other static assets if they exist
  (doseq [file (filter (every-pred File/.isFile
                                   (complement #((set (keys site/resource-destinations)) (File/.getName %))))
                       (.listFiles (io/file @site/*site-dir "resources")))]
    (u/copy-file file (site/public-html-file (.getName file)))))

(defn copy-assets!
  "Copy all assets from assets/ to TARGET/public_html/"
  []
  (log/in-section
    "Copying assets..."
    (let [assets-dir (site/site-file "assets")]
      (when (.exists assets-dir)
        (doseq [subdir (filter File/.isDirectory (.listFiles assets-dir))]
          (let [subdir-name (.getName subdir)
                target-dir (io/file (site/public-html-path subdir-name))]
            (log/log-action
              (str "copying " subdir-name "/")
              (io/make-parents (io/file target-dir "dummy"))
              (doseq [file (filter File/.isFile (file-seq subdir))]
                (let [relative-path (subs (.getPath file) (inc (count (.getPath subdir))))
                      target-file (io/file target-dir relative-path)]
                  (io/make-parents target-file)
                  (io/copy file target-file)
                  ;; Make CGI scripts executable
                  (when (= subdir-name "cgi-bin")
                    (.setExecutable target-file true)))))))))))

(defn copy-babashka!
  "Copy the current Babashka executable to TARGET/bin/"
  []
  (log/in-section
    "Copying Babashka executable..."
    (let [bb-path (first (filter #(.exists (io/file %))
                                 ["/usr/local/bin/bb"
                                  "/usr/bin/bb"
                                  "/opt/homebrew/bin/bb"
                                  (str (System/getenv "HOME") "/.nix-profile/bin/bb")
                                  (str (System/getenv "HOME") "/bin/bb")]))]
      (if bb-path
        (log/log-action
          (str "copy from " bb-path)
          (io/make-parents (site/target-path "bin" "bb"))
          (io/copy (io/file bb-path) (site/target-file "bin" "bb"))
          (.setExecutable (site/target-file "bin" "bb") true))
        (log/warn "⚠ Warning: Could not find Babashka executable to copy")))))

(defn setup-target!
  "Create the TARGET directory structure"
  []
  (log/in-section "Setting up TARGET directory structure..."
                  (doseq [dir site/target-dirs]
                    (u/ensure-directory (site/target-path dir)))))

(defn template-substitution [template options]
  (->> options
       (filter (comp string? val))
       (reduce #(str/replace %1 (re-pattern (str "\\{\\{" (name (key %2)) "}}")) (val %2)) template)))

(defn site-page-html [content file]
  (let [content (template-substitution content @site/*site-config)]
    (layout/site-page-layout @site/*site-config
                             (adoc/parse-asciidoc-header content)
                             (adoc/asciidoc-to-html content file))))

(defn process-site-page!
  "Process a single site page (about, contact, etc.)"
  [file]
  (let [filename (.getName (io/file file))
        basename (str/replace filename #"\.adoc$" "")
        file-name (str basename ".html")
        output-file (site/public-html-path file-name)]
    (log/log-action
      (str "processing " file-name)
      (io/make-parents output-file)
      (spit output-file (site-page-html (slurp file) file)))))

(defn parallel-build-pages [parallel? build!-fn files]
  (map (juxt File/.getName (if parallel? #(future (build!-fn %)) #(delay (build!-fn %)))) files))

(defn process-site-pages!
  "Process all pages in the site/ directory"
  [parallel?]
  (let [site-dir (site/site-file "site")]
    (when (.exists site-dir)
      (log/in-section
        (str "Processing site pages... parallel?=" parallel?)
        (let [ascii-doc-files (filter site/asciidoc-file-name? (.listFiles site-dir))]
          (doall
            (->>
              (log/monitor-parallel-actions!
                "AsciiDoc site pages"
                (delay (parallel-build-pages parallel? #(process-site-page! (.getPath %)) ascii-doc-files)))
              (mapv second)
              (mapv deref))))))))

(defn blog-post-html [content file]
  (let [meta (adoc/parse-asciidoc-header content)
        [html-content additional-css]
        (binding [highlighter/additional-header-css nil]
          [(adoc/asciidoc-to-html content file) highlighter/additional-header-css])]
    [(layout/blog-post-layout @site/*site-config meta html-content additional-css) meta]))

(defn process-blog-post!
  "Process a single blog post file"
  [file]
  (let [content (slurp file)
        filename (.getName (io/file file))
        basename (str/replace filename #"\.adoc$" "")
        output-file-name (str basename ".html")
        output-file (site/public-html-path "blog" output-file-name)]
    (log/log-action
      output-file-name
      (io/make-parents output-file)
      (let [[output meta] (blog-post-html content file)]
        (spit output-file output)
        {:file basename :meta meta}))))

(defn tag-index-html [tag posts]
  (let [sorted-posts (->> posts
                          (filter #(some #{tag} (get-in % [:meta :tags])))
                          (sort-by #(get-in % [:meta :date]))
                          reverse)]
    [(layout/tag-index-layout @site/*site-config tag sorted-posts) (count sorted-posts)]))

(defn generate-tag-index!
  "Generate an index page for a specific tag"
  [tag posts]
  (let [tag-slug (u/slugify tag)
        html-file-name (str tag-slug ".html")
        output-file (site/public-html-path "blog" "tags" html-file-name)
        [html n-tag-posts] (tag-index-html tag posts)]
    (log/log-action
      (str "generating tag index blog/tags/" html-file-name " (" n-tag-posts " posts)")
      (io/make-parents output-file)
      (spit output-file html))))

(defn generate-all-tag-indexes!
  "Generate index pages for all tags"
  [posts]
  (let [all-tags (distinct (flatten (map #(get-in % [:meta :tags]) posts)))]
    (when-let [valid-tags (seq (filter some? all-tags))]
      (log/in-section
        "Generating tag indexes..."
        (doseq [tag valid-tags]
          (generate-tag-index! tag posts))))))

(defn generate-index!
  "Generate the index page with all blog posts"
  [posts]
  (log/log-action
    "Generating index.html"
    (spit (site/public-html-path "index.html") (layout/index-layout @site/*site-config posts))))

(defn build! [& {:keys [parallel?] :or {parallel? true}}]
  (log/in-application
    "Building Static Blog"
    ;; Clean and setup TARGET
    (log/in-section "Setup from scratch"
                    (clean/clean-target!)
                    (setup-target!))
    ;; Copy static resources
    (copy-resources!)
    ;; Copy assets (css, js, img, cgi-bin)
    (copy-assets!)
    ;; Copy Babashka executable (optional - could be useful for CGI scripts)
    (copy-babashka!)
    ;; Process site pages (about, contact, etc.)
    (process-site-pages! parallel?)
    ;; Process blog posts
    (log/in-section
      "Processing blog posts..."
      (let [blog-dir (io/file @site/*site-dir "blog")
            blog-files (filter site/asciidoc-file-name?
                               (or (seq (.listFiles blog-dir)) []))
            posts (log/monitor-parallel-actions!
                    "AsciiDoc blog files"
                    (delay (parallel-build-pages parallel? #(process-blog-post! (.getPath %)) blog-files)))
            posts (map (comp deref second) posts)]
        ;; Generate index
        (log/in-section
          "Generating index and tag-index pages..."
          (generate-index! posts)
          (generate-all-tag-indexes! posts))

        (log/success "
        Generated " (count posts) " blog posts
        Website ready in TARGET/public_html/
        To deploy:
          1. Copy TARGET/public_html/* to your web server
          2. Ensure .htaccess is copied (it may be hidden)
        To test locally:
          clojure -T:build serve")))))