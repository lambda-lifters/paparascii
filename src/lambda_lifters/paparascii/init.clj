(ns lambda-lifters.paparascii.init
  (:require [clojure.pprint :as pp]
            [clojure.tools.logging :as log]
            [lambda-lifters.lambda-liftoff.io :as ll-io]
            [lambda-lifters.paparascii.site :as site])
  (:import (java.nio.file Path)))

(defn init!
  "Initialize a new site with basic structure"
  [& {:keys [name] :or {name "My Blog"}}]
  (log/info "Initializing new site: " name)
  (let [config {:site-name      name
                :copyright-date "2024"
                :contact-email  "email@example.com"
                :site-lead      "Welcome to my blog"
                :site-about     "A blog about interesting things"
                :links          [{:link-name "GitHub" :link-url "https://github.com"}
                                 {:link-name "Twitter" :link-url "https://twitter.com"}]}]
    (log/info "Create directories")
    (doseq [dir ["blog"
                 "site"
                 "resources"
                 "assets/css"
                 "assets/js"
                 "assets/img"
                 "assets/media"
                 "assets/fonts"
                 "templates"
                 ".github/workflows"]]
      (ll-io/ensure-directory dir))
    (log/info "Create site-config.edn")
    (spit "site-config.edn" (with-out-str (pp/pprint config)))
    (log/info "Copy default templates from resources")
    (when-let [template-site (ll-io/resource-path "template-site")]
      (doseq [template-file (file-seq (.toFile template-site))
              :when (.isFile template-file)]
        (let [relative-file (.toFile (Path/.relativize template-site (.toPath template-file)))
              destination-file (site/site-path relative-file)]
          (ll-io/copy-file template-file destination-file))))
    (when-let [workflow-template (ll-io/resource-path "template-github-workflow-build-and-deploy.yml")]
      (log/info "Copy GitHub workflow")
      (ll-io/copy-file (.toFile workflow-template)
                       (site/site-path ".github/workflows/build-and-deploy.yml")))
    (log/info "
      Site initialized! Next steps:
        1. Edit site-config.edn to customize your site
        2. Run: clojure -Tpaparascii build
        3. Run: clojure -Tpaparascii serve")))
