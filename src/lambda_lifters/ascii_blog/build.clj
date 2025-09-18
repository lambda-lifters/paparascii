(ns lambda-lifters.ascii-blog.build
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [hashp.preload]
            [hiccup2.core :as h]
            [lambda-lifters.ascii-blog.clean :as clean]
            [lambda-lifters.ascii-blog.log :as log]
            [lambda-lifters.ascii-blog.prism-js-highlighter :as highlighter]
            [lambda-lifters.ascii-blog.site :as site]
            [lambda-lifters.ascii-blog.util :as u])
  (:import (java.io File)
           (org.asciidoctor Asciidoctor$Factory Attributes Options)))

(defn copy-resources!
  "Copy all static resources to TARGET/public_html"
  []
  (log/section "Copying static resources...")
  (doseq [[src dst] site/resource-destinations]
    (u/copy-file (str @site/*site-dir "/resources/" src) (site/target-path dst)))

  ;; Copy any other static assets if they exist
  (doseq [file (filter (every-pred File/.isFile
                                   (complement #((set (keys site/resource-destinations)) (File/.getName %))))
                       (.listFiles (io/file @site/*site-dir "resources")))]
    (u/copy-file file (site/public-html-file (.getName file)))))

(defn copy-assets!
  "Copy all assets from assets/ to TARGET/public_html/"
  []
  (log/section "Copying assets...")
  (let [assets-dir (site/site-file "assets")]
    (when (.exists assets-dir)
      (doseq [subdir (filter File/.isDirectory (.listFiles assets-dir))]
        (let [subdir-name (.getName subdir)
              target-dir (io/file (site/public-html-path subdir-name))]
          (io/make-parents (io/file target-dir "dummy"))
          (doseq [file (filter File/.isFile (file-seq subdir))]
            (let [relative-path (subs (.getPath file) (inc (count (.getPath subdir))))
                  target-file (io/file target-dir relative-path)]
              (io/make-parents target-file)
              (io/copy file target-file)
              ;; Make CGI scripts executable
              (when (= subdir-name "cgi-bin")
                (.setExecutable target-file true))))
          (log/success "  ✓ Copied " (str subdir-name "/")))))))

(defn copy-babashka!
  "Copy the current Babashka executable to TARGET/bin/"
  []
  (log/section "Copying Babashka executable...")
  (let [bb-path (first (filter #(.exists (io/file %))
                               ["/usr/local/bin/bb"
                                "/usr/bin/bb"
                                "/opt/homebrew/bin/bb"
                                (str (System/getenv "HOME") "/.nix-profile/bin/bb")
                                (str (System/getenv "HOME") "/bin/bb")]))]
    (if bb-path
      (do
        (io/make-parents (site/target-path "bin" "bb"))
        (io/copy (io/file bb-path) (site/target-file "bin" "bb"))
        (.setExecutable (site/target-file "bin" "bb") true)
        (log/success "  ✓ Copied bb from " bb-path))
      (log/warn "  ⚠ Warning: Could not find Babashka executable to copy"))))

(defn setup-target!
  "Create the TARGET directory structure"
  []
  (log/section "Setting up TARGET directory structure...")
  (doseq [dir site/target-dirs]
    (u/ensure-directory (site/target-path dir)))
  (log/success "  ✓ Directory structure created"))

;; Create a singleton Asciidoctor instance
(def *asciidoctor (delay (let [doctor (Asciidoctor$Factory/create)]
                           (highlighter/register-highlighter-with-doctor doctor)
                           doctor)))

(def *asciidoctor-options (delay
                            (let [attributes
                                  (-> (Attributes/builder)
                                      (.backend "html5")
                                      (.showTitle false)    ; Don't show title (we handle it)
                                      (.noFooter true)
                                      (.linkCss true)
                                      (.copyCss true)
                                      (.sourceHighlighter highlighter/highlighter-name)
                                      (.attribute "sectids" false)
                                      .build)
                                  options (-> (Options/builder)
                                              (.standalone false) ; No header/footer
                                              (.attributes attributes))]
                              (.build options))))

(defn asciidoc-to-html
  "Convert AsciiDoc to HTML using AsciidoctorJ"
  [content & _]
  (.convert @*asciidoctor content @*asciidoctor-options))

(defn parse-asciidoc-header
  "Extract metadata from AsciiDoc header"
  [content]
  (let [lines (str/split-lines content)
        get-meta (fn [prefix]
                   (some #(when (str/starts-with? % prefix)
                            (str/trim (subs % (count prefix))))
                         lines))]
    {:title       (or (get-meta "= ") "Untitled")
     :author      (or (get-meta ":author:") "Anonymous")
     :date        (get-meta ":date:")
     :description (get-meta ":description:")
     :tags        (when-let [tags (get-meta ":tags:")] (str/split tags #",\s*"))}))

(defn head [title site-name description additional-head]
  [:head
   [:meta {:charset "UTF-8"}]
   [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
   [:title title " - " site-name]
   (when description [:meta {:name "description" :content "description "}])
   additional-head
   [:link {:href        "https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css"
           :rel         "stylesheet"
           :integrity   "sha384-T3c6CoIi6uLrA9TneNEoa7RxnatzjcDSCmG1MXxSR1GAsXEV/Dwwykc2MPK8M2HN"
           :crossorigin "anonymous"}
    [:link {:rel "stylesheet" :href "https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.2/font/bootstrap-icons.min.css"}]
    [:link {:rel "stylesheet" :href "/css/page-style.css"}]]])

(def navbar-sections [["/" "Home"] ["/blog" "Blog"] ["/about.html" "About"] ["/contact.html" "Contact"]])

(defn navbar [site-name]
  [:nav {:class "navbar navbar-expand-lg navbar-light"}
   [:div {:class "container"}
    [:a {:class "navbar-brand" :href (str "/")} site-name]
    [:button {:class "navbar-toggler" :type "button" :data-bs-toggle "collapse" :data-bs-target "#navbarNav"}
     [:span {:class "navbar-toggler-icon"}]]
    [:div {:class "collapse navbar-collapse" :id "navbarNav"}
     [:ul {:class "navbar-nav ms-auto"}
      (map #(vector :li {:class "nav-item"} [:a {:class "nav-link" :href (first %)} (second %)])
           navbar-sections)]]]])

(defn html-template
  "Generate complete HTML page with Bootstrap"
  [{:keys [title description content meta is-index? additional-head]}]
  (let [{:keys [site-name copyright-date contact-email links site-lead site-about]} @site/*site-config]
    (h/html
      (h/raw "<!DOCTYPE html>")
      [:html {:lang "en"}
       (head title site-name description additional-head)
       [:body
        (list
          (navbar site-name)
          [:div.content-wrapper content]
          [:footer
           [:div.container
            [:div.row
             [:div.col-md-4 [:h5 "About"] [:p site-about]]
             [:div.col-md-4 [:h5 "Links"] [:ul.list-unstyled
                                           (map (fn [{:keys [link-name link-url]}]
                                                  [:li [:a.text-white-50 {:href link-url} link-name]])
                                                links)]]
             [:div.col-md-4 [:h5 "Contact"] [:p.text-white-50 contact-email]]]
            [:hr.bg-white-50]
            [:div.text-center.text-white-50
             [:p "©" copyright-date " " site-name ". All rights reserved."]
             [:p "Built with: " [:a {:href "https://github.com/lambda-lifters/ascii-blog"} "The blog generator for AsciiDoc fans"]]]]]
          [:script {:src         "https://cdn.jsdelivr.net/npm/bootstrap @5.3.2/dist/js/bootstrap.bundle.min.js"
                    :integrity   "sha384-C6RzsynM9kWDrMNeT87bh95OGNyZPhcTNXj1NW7RuBCsyN/o0jlpcV8Qyq46cDfL"
                    :crossorigin "anonymous"}])]])))

(defn template-substitution [template options]
  (reduce #(str/replace %1 (re-pattern (str "\\{\\{" (name (key %2)) "}}")) (val %2)) template options))

(defn site-page-html [content file]
  (let [content (template-substitution content @site/*site-config)
        {:keys [title description] :as meta} (parse-asciidoc-header content)
        html-content (asciidoc-to-html content file)]
    (html-template
      {:title       title
       :description description
       :meta        meta
       :content     [:div.container.mt-4
                     [:article.blog-post
                      [:h2 title]
                      (h/raw html-content)]]})))

(defn process-site-page!
  "Process a single site page (about, contact, etc.)"
  [file]
  (let [filename (.getName (io/file file))
        basename (str/replace filename #"\.adoc$" "")
        file-name (str basename ".html")
        output-file (site/public-html-path file-name)]
    (io/make-parents output-file)
    (spit output-file (site-page-html (slurp file) file))
    (log/success "✓ Generated: " file-name)))

(defn process-site-pages!
  "Process all pages in the site/ directory"
  [parallel?]
  (let [site-dir (site/site-file "site")]
    (when (.exists site-dir)
      (log/section "Processing site pages...")
      (->> (seq (.listFiles site-dir))
           (filter site/asciidoc-file-name?)
           ((if parallel? pmap map) #(let [path (.getPath %)]
                                       (log/debug " ⚙" path)
                                       (process-site-page! path)))
           doall))))

(defn- tag-anchor [tag]
  [:a.tag {:href (str "/blog/tags/" (u/slugify tag) ".html")} tag])

(defn- tags-block [tags & classes]
  (when tags [:div.tags (when (seq classes) {:class (str/join classes " ")}) (map tag-anchor tags)]))

(defn- blog-meta-block [{:keys [date author]}]
  [:div.blog-meta
   [:i.bi.bi-calendar] date " • "
   [:i.bi.bi-person] author])

(defn blog-post-html [content file]
  (let [{:keys [title description tags] :as meta} (parse-asciidoc-header content)
        [html-content additional-css]
        (binding [highlighter/additional-header-css nil]
          [(asciidoc-to-html content file) highlighter/additional-header-css])]
    (html-template
      {:title           title
       :description     description
       :meta            meta
       :additional-head additional-css
       :content         [:div.container.mt-4
                         [:article.blog-post
                          [:h1 title]
                          (blog-meta-block meta)
                          (tags-block tags "mb-3")
                          (h/raw html-content)]]})))

(defn process-blog-post!
  "Process a single blog post file"
  [file]
  (let [content (slurp file)
        filename (.getName (io/file file))
        basename (str/replace filename #"\.adoc$" "")
        output-file-name (str basename ".html")
        output-file (site/public-html-path "blog" output-file-name)]
    (log/debug " ⚙" file)
    (io/make-parents output-file)
    (spit output-file (blog-post-html content file))
    (log/success "✓ Generated: " (.toString output-file))
    {:file basename :meta meta}))

(defn tag-hiccup-for-post [{:keys [meta file]}]
  (let [{:keys [title description tags]} meta]
    [:div.blog-post
     [:h3 [:a {:href (str "/blog/" file ".html")} title]]
     (blog-meta-block meta)
     (when description [:p description])
     (tags-block tags)]))

(defn tag-index-html [tag posts]
  (let [tag-posts (filter #(some #{tag} (get-in % [:meta :tags])) posts)
        sorted-posts (reverse (sort-by #(get-in % [:meta :date]) tag-posts))
        posts-hiccup (map tag-hiccup-for-post sorted-posts)
        n-tag-posts (count tag-posts)]
    [(html-template
       {:title       (str "Posts tagged: " tag)
        :description (str "All blog posts tagged with '" tag "'")
        :content     [:div.container.mt-4
                      [:h1 "Posts tagged: " [:span.tag {:style "font-size:1.5rem;"} tag]]
                      [:p.text-muted (str "Found " n-tag-posts " post"
                                          (when (not= 1 n-tag-posts) "s")
                                          " with this tag")]
                      [:hr.my-4]
                      (if (empty? tag-posts)
                        [:div.alert.alert-info "No posts found with this tag."]
                        posts-hiccup)
                      [:div.mt-4
                       [:a.btn.btn-primary {:href "/"} "← Back to Home"]]]})
     n-tag-posts]))

(defn generate-tag-index!
  "Generate an index page for a specific tag"
  [tag posts]
  (let [tag-slug (u/slugify tag)
        html-file-name (str tag-slug ".html")
        output-file (site/public-html-path "blog" "tags" html-file-name)
        [html n-tag-posts] (tag-index-html tag posts)]
    (io/make-parents output-file)
    (spit output-file html)
    (log/success " ✓ Generated tag index: blog/tags/" html-file-name " (" n-tag-posts " posts)")))

(defn generate-all-tag-indexes!
  "Generate index pages for all tags"
  [posts]
  (let [all-tags (distinct (flatten (map #(get-in % [:meta :tags]) posts)))]
    (when-let [valid-tags (seq (filter some? all-tags))]
      (log/section "Generating tag indexes...")
      (doseq [tag valid-tags]
        (generate-tag-index! tag posts)))))

(defn index-hiccup-for-post [post]
  [:div.blog-post
   [:h3 [:a {:href (str "/blog/" (:file post) ".html")}
         (get-in post [:meta :title])]]
   [:div.blog-meta
    [:i.bi.bi-calendar] (get-in post [:meta :date]) " • "
    [:i.bi.bi-person] (get-in post [:meta :author])]
   (when-let [description (get-in post [:meta :description])] [:p description])
   (when-let [tags (get-in post [:meta :tags])]
     [:div.tags (map #(vector :a.tag {:href (str "/blog/tags/" (u/slugify %) ".html")} %) tags)])])

(defn index-html [posts]
  (let [sorted-posts (reverse (sort-by #(get-in % [:meta :date]) posts))
        posts-hiccup (map index-hiccup-for-post sorted-posts)
        {:keys [site-name site-lead site-about]} @site/*site-config]
    (html-template
      {:title       (str site-name " - Home")
       :description "A modern blog about technology and development"
       :is-index?   true
       :content     (list
                      [:div.hero-section
                       [:div.container
                        [:h1.display-4 (str "Welcome to " site-name)]
                        [:p.lead site-lead]]]
                      [:div.container
                       [:div.row
                        [:div.col-lg-8
                         [:h2.mb-4 "Recent Posts"]
                         (if (empty? posts)
                           [:div.alert.alert-info "No blog posts yet. Add some .adoc files to the blog directory!"]
                           posts-hiccup)]
                        [:div.col-lg-4
                         [:div.card
                          [:div.card-body
                           [:h5.card-title "About This Blog"]
                           [:p.card-text site-about]]]
                         #_[:div.card.mt-3
                            [:div.card-body
                             [:h5.card-title "Categories"]
                             [:ul.list-unstyled
                              [:li [:a {:href "#"} "Technology"]]
                              [:li [:a {:href "#"} "Programming"]]
                              [:li [:a {:href "#"} "Web Development"]]]]]]]])})))

(defn generate-index!
  "Generate the index page with all blog posts"
  [posts]
  (spit (site/public-html-path "index.html") (index-html posts))
  (log/success "✓ Generated: index.html"))

(defn build! [& {:keys [parallel?] :or {parallel? true}}]
  (log/section "=== Building Static Blog ===")
  ;; Clean and setup TARGET
  (clean/clean-target!)
  (setup-target!)
  ;; Copy static resources
  (copy-resources!)
  ;; Copy assets (css, js, img, cgi-bin)
  (copy-assets!)
  ;; Copy Babashka executable (optional - could be useful for CGI scripts)
  (copy-babashka!)
  ;; Process site pages (about, contact, etc.)
  (process-site-pages! parallel?)
  ;; Process blog posts
  (log/section "Processing blog posts...")
  (let [blog-dir (io/file @site/*site-dir "blog")
        blog-files (filter site/asciidoc-file-name?
                           (or (seq (.listFiles blog-dir)) []))
        posts ((if parallel? pmap map) #(process-blog-post! (.getPath %)) blog-files)]
    ;; Generate index
    (log/section "Generating index page...")
    (generate-index! posts)
    ;; Generate tag indexes
    (generate-all-tag-indexes! posts)
    (log/success "=== Build Complete! ===")
    (log/success "Generated " (count posts) " blog posts")
    (log/log "Website ready in TARGET/public_html/")
    (log/log "To deploy:")
    (log/log "  1. Copy TARGET/public_html/* to your web server")
    (log/log "  2. Ensure .htaccess is copied (it may be hidden)")
    (log/log "To test locally:")
    (log/log "  clojure -T:build serve")))
