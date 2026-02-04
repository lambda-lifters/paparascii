(ns lambda-lifters.paparascii.build
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [lambda-lifters.paparascii.adoc :as adoc]
            [lambda-lifters.paparascii.file-system :as file-system]
            [lambda-lifters.paparascii.prism-js-highlighter :as highlighter]
            [lambda-lifters.paparascii.site :as site]
            [lambda-lifters.paparascii.site-layout :as layout]
            [lambda-lifters.paparascii.util :as u]
            [selmer.parser :as selmer]))

(defn site-page-html [content file]
  (let [site-config @site/*site-config
        content (selmer/render content site-config)
        header (adoc/parse-asciidoc-header content file site-config)
        content-html (adoc/asciidoc-to-html content file site-config)]
    {:slug         (u/slugify-file file)
     :header       header
     :html         (layout/site-page-layout site-config header content-html)
     :content-html content-html}))

(defn process-site-pages
  "Process all pages in the site/ directory
  Process a single site page (about, contact, etc.).
  Note that lead-article is special, in that it is rendered (if present) in the lead section of the page.
  It's not handled specially here, but this is where it comes into the system."
  [parallel?]
  (let [site-dir (site/site-file "site")]
    (when (.exists site-dir)
      (log/info (str "Processing site pages... parallel?=" parallel?))
      (let [ascii-doc-files (filter site/asciidoc-file-name? (.listFiles site-dir))]
        (into {}
              (comp
                (map #(site-page-html (slurp %) %))
                (map (juxt #(-> % :slug keyword) identity)))
              ascii-doc-files)))))

(defn render-blog-post
  "Process a single blog post file"
  [file]
  (let [content (slurp file)
        site-config @site/*site-config
        page-meta (adoc/parse-asciidoc-header content file site-config)
        additional-css nil
        html-content (binding [highlighter/additional-header-css additional-css]
                       (adoc/asciidoc-to-html content file site-config))]
    {:slug      (u/slugify-file file)
     :html      (layout/blog-post-layout site-config page-meta html-content additional-css)
     :page-meta page-meta}))

(defn generate-blog-posts []
  (log/info "Processing blog posts...")
  (let [blog-dir (io/file @site/*site-dir "blog")
        blog-files (filter site/asciidoc-file-name?
                           (or (seq (.listFiles blog-dir)) []))
        posts (map #(render-blog-post (.getPath %)) blog-files)]
    posts))

(defn generate-tag-indices [posts]
  (log/info "Generating tag indexes...")
  (for [tag (->> posts
                 (mapcat #(get-in % [:page-meta :tags]))
                 distinct
                 (filter some?))]
    (let [matching-posts (filter #(some #{tag} (get-in % [:page-meta :tags])) posts)
          {:keys [html n-tag-posts]} (layout/tag-index-html @site/*site-config tag matching-posts)]
      (log/info "generated tag index " tag "(" n-tag-posts "posts)")
      {:slug (u/slugify tag), :html html})))

(defn generate-index
  "Generate the index page with all blog posts.
  This is also the home page... so might include a lead document"
  [posts & {:keys [lead-doc] :as additional-lead-config}]
  (log/info "Generating index.html")
  (layout/index-layout (merge additional-lead-config @site/*site-config) posts))

(defn process-html-files [parallel?]
  (log/info "Processing blog posts...")
  (let [site-pages (process-site-pages parallel?)
        blog-posts (generate-blog-posts)
        index (generate-index blog-posts
                              :lead-article (-> site-pages :lead-article :content-html))
        tag-indices (generate-tag-indices blog-posts)]
    (doall (map file-system/write-blog-post! blog-posts))
    (doall (map file-system/write-site-page! (dissoc site-pages :lead-article)))
    (file-system/write-index! index)
    (doall (map file-system/write-tag-index! tag-indices))
    {:posts-processed (count blog-posts)}))

(defn build! [& {:keys [parallel?] :or {parallel? true}}]
  (log/info "Building Static Blog")
  (file-system/build-file-system!)
  (adoc/the-doctor)
  ;; Process site pages (about, contact, etc.)
  (log/info "Processing site pages")
  (let [report (process-html-files parallel?)]
    (log/info "
        Generated " (:posts-processed report) " blog posts
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
