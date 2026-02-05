(ns lambda-lifters.paparascii.site
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [lambda-lifters.lambda-liftoff.io :as ll-io]))

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

(defn existent-site-file
  "Build a file relative to the site directory, returning nil if it doesn't exist, or the file otherwise"
  [& parts]
  (let [f (apply site-file parts)]
    (when (.exists f) f)))

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
  (let [default-config-file (io/resource "site-config-defaults.edn") ; map be overwritten by "site-config.edn"
        config-file (site-path "site-config.edn")]
    (merge (ll-io/load-map-from-resource default-config-file "site-config-defaults.edn")
           (ll-io/load-map-from-resource config-file "site-config.edn"))))

(def *site-config (delay (load-config)))

(defn asciidoc-file-name? [file]
  (-> file
      .getName
      (str/ends-with? ".adoc")))
