(ns lambda-lifters.ascii-blog.init
  (:require [clojure.pprint :as pp]
            [lambda-lifters.ascii-blog.log :as log]
            [lambda-lifters.ascii-blog.site :as site]
            [lambda-lifters.ascii-blog.util :as u])
  (:import (java.nio.file Path)))

(defn init!
  "Initialize a new site with basic structure"
  [& {:keys [name] :or {name "My Blog"}}]
  (log/in-section
   (str "Initializing new site: " name)
   (let [config {:site-name name
                 :copyright-date "2024"
                 :contact-email "email@example.com"
                 :site-lead "Welcome to my blog"
                 :site-about "A blog about interesting things"
                 :links [{:link-name "GitHub" :link-url "https://github.com"}
                         {:link-name "Twitter" :link-url "https://twitter.com"}]}]
     (log/in-section
      "Create directories"
      (doseq [dir ["blog" "site" "resources" "assets/css" "assets/js" "assets/img" "templates" ".github/workflows"]]
        (u/ensure-directory dir)))
     (log/in-section
      "Create site-config.edn"
      (spit "site-config.edn" (with-out-str (pp/pprint config))))
     (log/in-section
      "Copy default templates from resources"

      (when-let [template-site (u/resource-path "template-site")]
        (doseq [template-file (file-seq (.toFile template-site))
                :when (.isFile template-file)]
          (let [relative-file (.toFile (Path/.relativize template-site (.toPath template-file)))
                destination-file (site/site-path relative-file)]
            (u/copy-file template-file destination-file)))))
     (log/in-section
      "Copy GitHub workflow"
      (when-let [workflow-template (u/resource-path "template-github-workflow-build-and-deploy.yml")]
        (u/copy-file (.toFile workflow-template) (site/site-path ".github/workflows/build-and-deploy.yml"))))
     (log/success "
      Site initialized! Next steps:
        1. Edit site-config.edn to customize your site
        2. Run: clojure -Tascii-blog build
        3. Run: clojure -Tascii-blog serve"))))