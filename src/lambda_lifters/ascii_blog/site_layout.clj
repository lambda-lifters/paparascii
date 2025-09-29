(ns lambda-lifters.ascii-blog.site-layout
  (:require [clojure.string :as str]
            [hiccup2.core :as h]
            [lambda-lifters.ascii-blog.util :as u]))

(def bootstrap {
                :stylesheet {:href      "https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css"
                             :integrity "sha384-T3c6CoIi6uLrA9TneNEoa7RxnatzjcDSCmG1MXxSR1GAsXEV/Dwwykc2MPK8M2HN"}
                :icon-css   "https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.2/font/bootstrap-icons.min.css"
                :script     {:src       "https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"
                             :integrity "sha384-C6RzsynM9kWDrMNeT87bh95OGNyZPhcTNXj1NW7RuBCsyN/o0jlpcV8Qyq46cDfL"}
                })

(def navbar-sections [["/" "Home"]
                      ["/blog" "Blog"]
                      ["/about.html" "About"]
                      ["/contact.html" "Contact"]])

(defn head [title site-name description additional-head]
  [:head
   [:meta {:charset "UTF-8"}]
   [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
   [:title title " - " site-name]
   (when description [:meta {:name "description" :content "description "}])
   additional-head
   [:link (assoc (:stylesheet bootstrap) :rel "stylesheet" :crossorigin "anonymous")]
   [:link {:rel "stylesheet" :href (:icon-css bootstrap)}]
   [:link {:rel "stylesheet" :href "/css/page-style.css"}]])

(defn navbar [site-name]
  [:nav {:class "navbar navbar-expand-lg navbar-light"}
   [:div {:class "container"}
    [:a {:class "navbar-brand" :href "/"} site-name]
    [:button {:class "navbar-toggler" :type "button" :data-bs-toggle "collapse" :data-bs-target "#navbarNav"}
     [:span {:class "navbar-toggler-icon"}]]
    [:div {:class "collapse navbar-collapse" :id "navbarNav"}
     [:ul {:class "navbar-nav ms-auto"}
      (map #(vector :li {:class "nav-item"} [:a {:class "nav-link" :href (first %)} (second %)]) navbar-sections)]]]])

(defn footer [site-about links contact-email copyright-date site-name]
  (list
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
       [:p "Built with: " [:a {:href "https://github.com/lambda-lifters/ascii-blog"}
                           [:b "ASCII Blog:"] " The blog generator for AsciiDoc fans"]]]]]
    [:script (assoc (:script bootstrap) :crossorigin "anonymous")]))

(defn html-template
  "Generate complete HTML page with Bootstrap"
  [site-config {:keys [title description content additional-head]}]
  (let [{:keys [site-name copyright-date contact-email links site-about]} site-config]
    (h/html
      (h/raw "<!DOCTYPE html>")
      [:html {:lang "en"}
       (head title site-name description
             (concat additional-head
                     (u/handle-config-markup-directives
                       (:additional-header-content site-config))))
       [:body
        (list
          (navbar site-name)
          [:div.content-wrapper content]
          (footer site-about links contact-email copyright-date site-name))]])))

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

(defn index-layout [site-config posts]
  (let [{:keys [site-name site-lead site-about site-description]} site-config]
    (html-template
      site-config
      {:title       (str site-name " - Home")
       :description site-description
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
                           (->> posts
                                (sort-by (comp :date :meta))
                                reverse
                                (map #(index-entry-for-post % (:meta %)))))]
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

(defn site-page-layout [site-config {:keys [title description] :as meta} rendered-html]
  (html-template
    site-config
    {:title       title
     :description description
     :meta        meta
     :content     [:div.container.mt-4
                   [:article.blog-post
                    [:h2 title]
                    (h/raw rendered-html)]]}))

(defn- tags-block [tags & classes]
  (when tags [:div.tags (when (seq classes) {:class (str/join classes " ")}) (map tag-anchor tags)]))

(defn- blog-meta-block [{:keys [date author]}]
  [:div.blog-meta [:i.bi.bi-calendar] " " date " • " [:i.bi.bi-person] " " author])


(defn blog-post-layout [{:keys [post-additional-header-content post-article-content] :as site-config}
                        {:keys [title description tags] :as meta}
                        rendered-html additional-css]
  (html-template
    site-config
    {:title           title
     :description     description
     :meta            meta
     :additional-head (conj (u/handle-config-markup-directives post-additional-header-content) additional-css)
     :content         [:div.container.mt-4
                       [:article.blog-post
                        [:h1 title]
                        (blog-meta-block meta)
                        (tags-block tags "mb-3")
                        (h/raw rendered-html)]
                       post-article-content]}))

(defn tag-hiccup-for-post [{:keys [meta file]}]
  (let [{:keys [title description tags]} meta]
    [:div.blog-post
     [:h3 [:a {:href (blog-url file)} title]]
     (blog-meta-block meta)
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