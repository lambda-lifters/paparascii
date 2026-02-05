(ns lambda-lifters.paparascii.init
  (:require [clojure.pprint :as pp]
            [clojure.tools.logging :as log]
            [lambda-lifters.lambda-liftoff.io :as ll-io]
            [lambda-lifters.paparascii.file-system :as fs]
            [lambda-lifters.paparascii.site :as site]))

(def seed-directories ["blog"
                       "site"
                       "resources"
                       "assets/css"
                       "assets/js"
                       "assets/img"
                       "assets/media"
                       "assets/fonts"
                       "templates"
                       ".github/workflows"])

(def seed-config {:site-name      name
                  :copyright-date "2024"
                  :contact-email  "email@example.com"
                  :site-lead      "Welcome to my blog"
                  :site-about     "A blog about interesting things"
                  :links          [{:link-name "GitHub" :link-url "https://github.com"}
                                   {:link-name "Twitter" :link-url "https://twitter.com"}]})

(defn init!
  "Initialize a new site with basic structure"
  [& {:keys [name] :or {name "My Blog"}}]
  (log/info "Initializing new site: " name)
  (let [config seed-config]
    (log/info "Create directories")
    (dorun (->> seed-directories
                (map ll-io/ensure-directory)))
    (log/info "Create site-config.edn")
    (-> (with-out-str (pp/pprint config))
        (spit "site-config.edn"))
    (log/info "Copy default templates from resources")
    (when-let [template-site (ll-io/resource-path "template-site")]
      (let [transpose-template-to-site #(ll-io/copy-file % (fs/transpose-file template-site site/site-path %))]
        (dorun (->> template-site
                    .toFile
                    file-seq
                    (filter .isFile)
                    (map transpose-template-to-site)))))
    (when-let [workflow-template (ll-io/resource-path "template-github-workflow-build-and-deploy.yml")]
      (log/info "Copy GitHub workflow")
      (ll-io/copy-file (.toFile workflow-template)
                       (site/site-path ".github/workflows/build-and-deploy.yml")))
    (log/info "
      Site initialized! Next steps:
        1. Edit site-config.edn to customize your site
        2. Run: clojure -Tpaparascii build
        3. Run: clojure -Tpaparascii serve")))
