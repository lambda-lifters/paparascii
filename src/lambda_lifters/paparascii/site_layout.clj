(ns lambda-lifters.paparascii.site-layout
  (:require [hiccup2.core :refer [raw]]
            [lambda-lifters.paparascii.hiccup-layout :as layout]
            [lambda-lifters.paparascii.util :refer [slugify]]
            [selmer.parser :refer [render]]))

((requiring-resolve 'hashp.install/install!))

(defn html-template-head [{:keys [additional-header-content] :as site-config}
                          {:keys [description additional-head] :as page-meta}]
  (let [merged-data (merge site-config page-meta)
        raw-title (raw (render (:head-title site-config) merged-data))
        more-raw-header (->> (concat additional-head
                                     (map #(render % merged-data) additional-header-content))
                             (map raw))]
    (apply layout/html-template-head-layout raw-title description more-raw-header)))

(defn html-template-body [{:keys [site-name navbar-sections]} {:keys [content]}]
  (layout/html-template-body-layout (raw site-name) navbar-sections content))

(defn- html-template-foot [site-config]
  (let [{:keys [contact-email links site-about
                footer-about-title
                footer-links-title
                footer-contact-title
                footer-copyright-template
                footer-paparascii-advert-template]} site-config
        raw-rendered (partial (comp raw #(render % site-config)))]
    (layout/html-template-foot-layout
      (raw footer-about-title)
      (raw site-about)
      (raw footer-links-title)
      (map #(update % :link-url raw-rendered) links)
      (raw footer-contact-title)
      (raw contact-email)
      (map raw-rendered [footer-copyright-template footer-paparascii-advert-template]))))

(defn html-template
  "Generate complete HTML page with Bootstrap"
  [site-config page-meta]
  (layout/html-template-layout
    (html-template-head site-config page-meta)
    (html-template-body site-config page-meta)
    (html-template-foot site-config)))

(def tag-url #(str "/blog/tags/" (slugify %) ".html"))

(def blog-url #(str "/blog/" % ".html"))

(defn tag-anchor [tag] (layout/tag-anchor-layout (tag-url tag) tag))

(defn index-entry-for-post [{:keys [slug] :as _post} {:keys [title date author description tags] :as _meta}]
  (layout/index-entry-for-post-layout (blog-url slug) title date author description (map tag-anchor tags)))

(defn index-content [{:keys [site-lead site-about index-welcome-template about-card-title lead-article] :as site-config} posts]
  (let [rendered-posts (map #(index-entry-for-post % (:page-meta %))
                            (reverse (sort-by (comp :date :page-meta) posts)))
        about-card (when site-about {:text (raw site-about)
                                     :title (raw about-card-title)
                                     :lead-article (raw lead-article)})]
    (layout/index-content-layout (raw (render index-welcome-template site-config)) (raw site-lead) rendered-posts about-card)))

(defn index-layout [{:keys [site-description index-title-template] :as site-config} posts]
  (html-template
    site-config
    {:title       (raw (render index-title-template site-config))
     :description (raw site-description)
     :is-index?   true
     :content     (index-content site-config posts)}))

(defn site-page-layout [site-config {:keys [title description] :as page-meta} rendered-html]
  (html-template
    site-config
    {:title       title
     :description description
     :page-meta   page-meta
     :content     (layout/site-page-content-layout title (raw rendered-html))}))

(defn tags-block [tags & {:as options}]
  (when tags (layout/tags-block-layout (map tag-anchor tags) options)))

(defn blog-meta-block [{:keys [date author]}]
  (layout/blog-meta-block-layout date author))

(defn blog-post-layout [site-config page-meta rendered-html additional-css]
  (let [merged-data (merge site-config page-meta)
        {:keys [post-additional-header-content-templates post-article-content]} site-config
        {:keys [title description tags]} page-meta
        content (layout/blog-post-content-layout
                  title
                  (blog-meta-block page-meta)
                  (tags-block tags :pad-below? true)
                  (raw rendered-html)
                  (map raw post-article-content))]
    (html-template
      site-config
      {:title           title
       :description     description
       :page-meta       page-meta
       :additional-head (conj (map #(raw (render % merged-data)) post-additional-header-content-templates)
                              additional-css)
       :content         content})))

(defn tag-hiccup-for-post [{:keys [page-meta file]}]
  (let [{:keys [title description tags]} page-meta]
    (layout/tagged-post-index-entry-layout (blog-url file) title (blog-meta-block page-meta) description (tags-block tags))))

(defn tag-index-layout [site-config tag sorted-posts]
  (let [n-tag-posts (count sorted-posts)
        rendered-posts (map tag-hiccup-for-post sorted-posts)
        content (layout/tag-index-content-layout n-tag-posts tag rendered-posts)]
    (html-template
      site-config
      {:title       (str "Posts tagged: " tag)
       :description (list "All blog posts tagged with " [:q tag] "")
       :content     content})))

(defn tag-index-html [config tag matching-posts]
  (let [sorted-posts (->> matching-posts
                          (sort-by #(get-in % [:page-meta :date]))
                          reverse)]
    {:html        (tag-index-layout config tag sorted-posts)
     :n-tag-posts (count sorted-posts)}))
