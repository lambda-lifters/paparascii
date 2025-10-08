(ns lambda-lifters.paparascii.site
  (:require [lambda-lifters.paparascii.log :as log]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (java.io PushbackReader)))

(defn resolve-site-dir [& _]
  (or (System/getProperty "SITE_DIR")
      (System/getenv "SITE_DIR")
      "."))

(def *site-dir
  "Get the site directory from environment or current directory"
  (atom (resolve-site-dir)))

(defn site-file
  "Build a file relative to the site directory"
  [& parts]
  (apply io/file @*site-dir parts))

(defn site-path
  "Build a path relative to the site directory"
  [& parts]
  (str (apply site-file parts)))

(def target-path
  "Build a path relative to the TARGET directory"
  (partial site-path "TARGET"))

(def public-html-path
  "Build a path relative to the TARGET/public_html directory"
  (partial target-path "public_html"))

(def target-file
  "Build a file relative to the TARGET directory"
  (partial site-file "TARGET"))

(def public-html-file
  "Build a file relative to the TARGET/public_html directory"
  (partial target-file "public_html"))

(def resource-destinations
  {"htaccess" "public_html/.htaccess"                       ; Copy .htaccess (renamed from htaccess)
   "404.html" "public_html/404.html"
   "500.html" "public_html/500.html"})

(def target-dirs #{"public_html/blog"
                   "public_html/blog/tags"
                   "public_html/css"
                   "public_html/js"
                   "public_html/img"
                   "public_html/media"
                   "public_html/fonts"
                   "public_html/cgi-bin"
                   "bin"
                   "logs"})

(defn load-config
  "Load site configuration from site-config.edn"
  []
  (let [config-file (site-path "site-config.edn")]
    (try
      (let [config (with-open [rdr (PushbackReader. (io/reader config-file))] (edn/read rdr))]
        (when-not (map? config) (throw (ex-info "site-config.edn is not a map" {:parsed-config config})))
        config)
      (catch Exception e
        (log/error (str "Error reading config file: " (.getMessage e)))
        (throw e)))))

(def *site-config (delay (load-config)))

(defn asciidoc-file-name? [file]
  (str/ends-with? (.getName file) ".adoc"))
