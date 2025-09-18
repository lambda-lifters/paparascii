(ns lambda-lifters.ascii-blog.init
  (:require [clojure.pprint :as pp]
            [lambda-lifters.ascii-blog.log :as log]
            [lambda-lifters.ascii-blog.site :as site]
            [lambda-lifters.ascii-blog.util :as u])
  (:import (java.nio.file Path)))

(defn init!
  "Initialize a new site with basic structure"
  [& {:keys [name] :or {name "My Blog"}}]
  (log/section "Initializing new site:" name)
  (let [config {:site-name      name
                :copyright-date "2024"
                :contact-email  "email@example.com"
                :site-lead      "Welcome to my blog"
                :site-about     "A blog about interesting things"
                :links          [{:link-name "GitHub" :link-url "https://github.com"}
                                 {:link-name "Twitter" :link-url "https://twitter.com"}]}]
    ;; Create directories
    (doseq [dir ["blog" "site" "resources" "assets/css" "assets/js" "assets/img" "templates"]]
      (u/ensure-directory dir))
    ;; Create site-config.edn
    (spit "site-config.edn" (with-out-str (pp/pprint config)))
    (log/success "  ✓ Created site-config.edn")
    ;; Copy default templates from resources
    (when-let [template-site (u/resource-path "template-site")]
      (doseq [template-file (file-seq (.toFile template-site))
              :when (.isFile template-file)]
        (let [relative-file (.toFile (Path/.relativize template-site (.toPath template-file)))
              destination-file (site/site-path relative-file)]
          (u/copy-file template-file destination-file)))
      (log/success "  ✓ Copied default template"))

    (log/success "Site initialized! Next steps:")
    (log/success "  1. Edit site-config.edn to customize your site")
    (log/success "  2. Run: clojure -Tascii-blog build")
    (log/success "  3. Run: clojure -Tascii-blog serve")))

