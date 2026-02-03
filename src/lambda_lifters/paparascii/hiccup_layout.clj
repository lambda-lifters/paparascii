(ns lambda-lifters.paparascii.hiccup-layout
  (:require [hiccup2.core :as h]))

(def doctype (h/raw "<!DOCTYPE html>"))
(def &nbsp (h/raw "&nbsp;"))
(def &middot (h/raw "&middot;"))

(def bootstrap {
                :stylesheet {:href      "https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css"
                             :integrity "sha384-T3c6CoIi6uLrA9TneNEoa7RxnatzjcDSCmG1MXxSR1GAsXEV/Dwwykc2MPK8M2HN"}
                :icon-css   "https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.2/font/bootstrap-icons.min.css"
                :script     {:src       "https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"
                             :integrity "sha384-C6RzsynM9kWDrMNeT87bh95OGNyZPhcTNXj1NW7RuBCsyN/o0jlpcV8Qyq46cDfL"}
                })

(defn html-template-head-layout [title description & more-raw-header]
  [:head
   [:meta {:charset "UTF-8"}]
   [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
   [:title title]
   (when description [:meta {:name "description" :content description}])
   [:link (assoc (:stylesheet bootstrap) :rel "stylesheet" :crossorigin "anonymous")]
   [:link {:rel "stylesheet" :href (:icon-css bootstrap)}]
   [:link {:rel "stylesheet" :href "/css/font-awesome.css"}]
   [:link {:rel "stylesheet" :href "/css/page-style.css"}]
   [:link {:rel "stylesheet" :href "/css/paparascii-generated.css"}]
   more-raw-header])

(defn navbar-section-layout [[href content]]
  [:li.nav-item
   [:a.nav-link {:href href}
    content]])

(defn navbar-layout [site-name navbar-sections]
  [:nav.navbar.navbar-expand-lg.navbar-light
   [:div.container
    [:a.navbar-brand {:href "/"} site-name]
    [:button.navbar-toggler {:type "button" :data-bs-toggle "collapse" :data-bs-target "#navbarNav"}
     [:span.navbar-toggler-icon]]
    [:div#navbarNav.collapse.navbar-collapse
     [:ul.navbar-nav.ms-auto (map navbar-section-layout navbar-sections)]]]])

(defn html-template-body-layout [site-name navbar-sections content]
  [:body
   (navbar-layout site-name navbar-sections)
   [:div.content-wrapper
    content]])

(defn footer-link-layout [{:keys [link-name link-url]}]
  [:li
   [:a.text-white-50 {:href link-url}
    link-name]])

(defn html-template-foot-layout [about-title site-about links-title links contact-title contact-email more-content]
  (list
    [:footer
     [:div.container
      [:div.row
       [:div.col-md-4
        [:h5 about-title]
        [:p site-about]]
       [:div.col-md-4
        [:h5 links-title]
        [:ul.list-unstyled
         (map footer-link-layout links)]]
       [:div.col-md-4
        [:h5 contact-title]
        [:p.text-white-50 contact-email]]]
      [:hr.bg-white-50]
      [:div.text-center.text-white-50
       more-content]]]
    [:script (assoc (:script bootstrap) :crossorigin "anonymous")]))

(defn html-template-layout [head body footer]
  (h/html doctype [:html {:lang "en"} head body footer]))

(defn tag-anchor-layout [url tag] [:a.tag {:href url} tag])

(defn index-entry-for-post-layout [url title date author maybe-description maybe-tag-anchors]
  [:div.blog-post.col-md-4
   [:h3 [:a {:href url} title]]
   [:div.blog-meta
    (when date (list [:i.bi.bi-calendar] &nbsp date))
    (when (and date author) (list " " &middot " "))
    (when author (list [:i.bi.bi-person] &nbsp author))]
   (when maybe-description [:p maybe-description])
   (when (seq maybe-tag-anchors) [:div.tags maybe-tag-anchors])])

(defn index-content-layout [welcome site-lead posts {:keys [title text] :as site-about}]
  (list
    [:div.hero-section
     [:div.container
      [:h1.display-4 welcome]
      [:p.lead site-lead]]]
    [:div.container
     [:div.row
      [:h2.mb-4 "Recent Posts"]
      (if (empty? posts)
        [:div.alert.alert-info "No blog posts yet. Add some .adoc files to the blog directory!"]
        posts)
      (when site-about
        [:div.col-md-4
         [:div.card
          [:div.card-body
           [:h5.card-title title]
           [:p.card-text text]]]])]]))

(defn site-page-content-layout [title rendered-html]
  [:div.container.mt-4
   [:article.blog-post
    [:h2 title]
    rendered-html]])

(defn blog-meta-block-layout [date author]
  [:div.blog-meta
   [:i.bi.bi-calendar] " " date
   " • "
   [:i.bi.bi-person] " " author])

(defn tags-block-layout [tags & {:keys [pad-below?]}]
  [:div.tags (when pad-below? {:class "mb-3"})
   tags])

(defn blog-post-content-layout [title meta-block tags rendered-html post-article-content]
  [:div.container.mt-4
   [:article.blog-post
    [:h1 title]
    meta-block
    tags
    rendered-html]
   post-article-content])

(defn tagged-post-index-entry-layout [url title meta-block description tags]
  [:div.blog-post
   [:h3
    [:a {:href url} title]]
   meta-block
   (when description [:p description])
   tags])

(defn tag-index-content-layout [n-tag-posts tag posts]
  [:div.container.mt-4
   [:h1 "Posts tagged: "
    [:span.tag {:style "font-size:1.5rem;"} tag]]
   [:p.text-muted (str "Found " n-tag-posts " post"
                       (when (not= 1 n-tag-posts) "s")
                       " with this tag")]
   [:hr.my-4]
   (if (empty? posts)
     [:div.alert.alert-info "No posts found with this tag."] posts)
   [:div.mt-4
    [:a.btn.btn-primary {:href "/"} "← Back to Home"]]])
