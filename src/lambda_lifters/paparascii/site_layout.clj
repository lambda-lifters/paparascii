(ns lambda-lifters.paparascii.site-layout
  (:require [clojure.string :as str]
            [hiccup2.core :as h]
            [lambda-lifters.paparascii.util :as u]
            [selmer.parser :as selmer]))

(def bootstrap {
                :stylesheet {:href      "https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css"
                             :integrity "sha384-T3c6CoIi6uLrA9TneNEoa7RxnatzjcDSCmG1MXxSR1GAsXEV/Dwwykc2MPK8M2HN"}
                :icon-css   "https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.2/font/bootstrap-icons.min.css"
                :script     {:src       "https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"
                             :integrity "sha384-C6RzsynM9kWDrMNeT87bh95OGNyZPhcTNXj1NW7RuBCsyN/o0jlpcV8Qyq46cDfL"}
                })

(defn navbar [site-name navbar-sections]
  [:nav.navbar.navbar-expand-lg.navbar-light
   [:div.container
    [:a.navbar-brand {:href "/"} (h/raw site-name)]
    [:button.navbar-toggler {:type "button" :data-bs-toggle "collapse" :data-bs-target "#navbarNav"}
     [:span.navbar-toggler-icon]]
    [:div.collapse.navbar-collapse {:id "navbarNav"}
     [:ul.navbar-nav.ms-auto
      (map #(vector :li.nav-item [:a.nav-link {:href (first %)} (second %)]) navbar-sections)]]]])

(defn html-template
  "Generate complete HTML page with Bootstrap"
  [site-config {:keys [description content additional-head] :as page-meta}]
  (let [{:keys [site-name contact-email links site-about
                additional-header-content
                footer-about-title
                footer-links-title
                footer-contact-title
                footer-copyright-template
                footer-paparascii-advert-template
                navbar-sections]} site-config]
    (h/html
      (h/raw "<!DOCTYPE html>")
      [:html {:lang "en"}
       ;; --------------------
       [:head
        [:meta {:charset "UTF-8"}]
        [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
        [:title (h/raw (selmer/render (:head-title site-config) (merge site-config page-meta)))]
        (when description [:meta {:name "description" :content description}])
        (concat additional-head
                (map #(h/raw (selmer/render % (merge site-config page-meta))) additional-header-content))
        [:link (assoc (:stylesheet bootstrap) :rel "stylesheet" :crossorigin "anonymous")]
        [:link {:rel "stylesheet" :href (:icon-css bootstrap)}]
        [:link {:rel "stylesheet" :href "/css/font-awesome.css"}]
        [:link {:rel "stylesheet" :href "/css/page-style.css"}]]
       ;; --------------------
       [:body
        (navbar site-name navbar-sections)
        [:div.content-wrapper content]]
       ;; --------------------
       (list
         [:footer
          [:div.container
           [:div.row
            [:div.col-md-4 [:h5 (h/raw footer-about-title)]
             [:p (h/raw site-about)]]
            [:div.col-md-4 [:h5 (h/raw footer-links-title)]
             [:ul.list-unstyled
              (map (fn [{:keys [link-name link-url]}]
                     [:li [:a.text-white-50 {:href (h/raw (selmer/render link-url site-config))} link-name]])
                   links)]]
            [:div.col-md-4 [:h5 (h/raw footer-contact-title)]
             [:p.text-white-50 (h/raw contact-email)]]]
           [:hr.bg-white-50]
           [:div.text-center.text-white-50
            (h/raw (selmer/render footer-copyright-template site-config))
            (h/raw (selmer/render footer-paparascii-advert-template site-config))]]]
         [:script (assoc (:script bootstrap) :crossorigin "anonymous")])])))

(defn tag-url [tag] (str "/blog/tags/" (u/slugify tag) ".html"))

(defn blog-url [file] (str "/blog/" file ".html"))

(defn- tag-anchor [tag]
  [:a.tag {:href (tag-url tag)} tag])

(defn index-entry-for-post [{:keys [file] :as _post} {:keys [title date author description tags] :as _meta}]
  [:div.blog-post
   [:h3 [:a {:href (blog-url file)} title]]
   [:div.blog-meta
    [:i.bi.bi-calendar] date " • "
    [:i.bi.bi-person] author]
   (when description [:p description])
   (when tags [:div.tags (map tag-anchor tags)])])

(defn- index-content [{:keys [site-lead site-about index-welcome-template about-card-title] :as site-config} posts]
  (list
    [:div.hero-section
     [:div.container
      [:h1.display-4 (h/raw (selmer/render index-welcome-template site-config))]
      [:p.lead (h/raw site-lead)]]]
    [:div.container
     [:div.row
      [:div.col-lg-8
       [:h2.mb-4 "Recent Posts"]
       (if (empty? posts)
         [:div.alert.alert-info "No blog posts yet. Add some .adoc files to the blog directory!"]
         (->> posts
              (sort-by (comp :date :page-meta))
              reverse
              (map #(index-entry-for-post % (:page-meta %)))))]
      (when site-about [:div.col-lg-4
                        [:div.card
                         [:div.card-body
                          [:h5.card-title (h/raw about-card-title)]
                          [:p.card-text (h/raw site-about)]]]])]]))

(defn index-layout [{:keys [site-description index-title-template] :as site-config} posts]
  (html-template
    site-config
    {:title       (h/raw (selmer/render index-title-template site-config))
     :description (h/raw site-description)
     :is-index?   true
     :content     (index-content site-config posts)}))

(defn site-page-layout [site-config {:keys [title description] :as page-meta} rendered-html]
  (html-template
    site-config
    {:title       title
     :description description
     :page-meta   page-meta
     :content     [:div.container.mt-4 [:article.blog-post [:h2 title] (h/raw rendered-html)]]}))

(defn- tags-block [tags & classes]
  (when tags [:div.tags (when (seq classes) {:class (str/join classes " ")}) (map tag-anchor tags)]))

(defn- blog-meta-block [{:keys [date author]}]
  [:div.blog-meta [:i.bi.bi-calendar] " " date " • " [:i.bi.bi-person] " " author])


(defn blog-post-layout [{:keys [post-additional-header-content-templates post-article-content] :as site-config}
                        {:keys [title description tags] :as page-meta}
                        rendered-html additional-css]
  (html-template
    site-config
    {:title           title
     :description     description
     :page-meta       page-meta
     :additional-head (conj
                        (map #(h/raw (selmer/render % (merge site-config page-meta))) post-additional-header-content-templates)
                        additional-css)
     :content         [:div.container.mt-4
                       [:article.blog-post
                        [:h1 title]
                        (blog-meta-block page-meta)
                        (tags-block tags "mb-3")
                        (h/raw rendered-html)]
                       (map h/raw post-article-content)]}))

(defn tag-hiccup-for-post [{:keys [page-meta file]}]
  (let [{:keys [title description tags]} page-meta]
    [:div.blog-post
     [:h3 [:a {:href (blog-url file)} title]]
     (blog-meta-block page-meta)
     (when description [:p description])
     (tags-block tags)]))

(defn tag-index-layout [site-config tag sorted-posts]
  (let [n-tag-posts (count sorted-posts)]
    (html-template
      site-config
      {:title       (str "Posts tagged: " tag)
       :description (list "All blog posts tagged with " [:q tag] "")
       :content     [:div.container.mt-4
                     [:h1 "Posts tagged: " [:span.tag {:style "font-size:1.5rem;"} tag]]
                     [:p.text-muted (str "Found " n-tag-posts " post"
                                         (when (not= 1 n-tag-posts) "s")
                                         " with this tag")]
                     [:hr.my-4]
                     (if (empty? sorted-posts)
                       [:div.alert.alert-info "No posts found with this tag."]
                       (map tag-hiccup-for-post sorted-posts))
                     [:div.mt-4 [:a.btn.btn-primary {:href "/"} "← Back to Home"]]]})))
