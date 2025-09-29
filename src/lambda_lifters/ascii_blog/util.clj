(ns lambda-lifters.ascii-blog.util
  (:require [clojure.data.json :as json]
            [clojure.walk :as walk]
            [hiccup2.core :as h]
            [lambda-lifters.ascii-blog.log :as log]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str])
  (:import (java.nio.file Path)))

(defn slugify
  "Convert a title to a URL- (and file-) friendly slug"
  [title]
  (-> title
      str/lower-case
      (str/replace #"[^\w \s-]" "")                         ; Remove non-word chars except spaces and hyphens
      (str/replace #"\s+" "-")                              ; Replace spaces with hyphens
      (str/replace #"-+" "-")                               ; Replace multiple hyphens with single
      (str/replace #"^-|-$" "")))                           ; Remove leading/trailing hyphens

(defn copy-file
  "Copy a file from source to destination"
  [src dest]
  (log/log-action
    (str "copy " src " → " (.toString dest))
    (io/make-parents dest)
    (io/copy (io/file src)
             (io/file dest))))

(defn ensure-directory [dir]
  (log/log-action
    (str "ensure directory " dir)
    (io/make-parents (io/file dir "dummy"))
    (.delete (io/file dir "dummy"))))

(defn resource-path [rsrc]
  (Path/of (.toURI (io/resource rsrc))))

(defn get-git-user-name
  "Get the user.name from git config, or fallback to system user"
  []
  (try
    (let [result (shell/sh "git" "config" "user.name")
          username (str/trim (:out result))]
      (if (str/blank? username)
        (System/getProperty "user.name" "Anonymous")
        username))
    (catch Exception _
      (System/getProperty "user.name" "Anonymous"))))

(defn handle-config-markup-directives [s & {:keys [title]}]
  (walk/postwalk #(cond (not (vector? %)) %
                        (= (first %) :->raw) (h/raw (apply str (rest %)))
                        (= (first %) :->json) (json/write-str (second %))
                        (= (first %) :->slug) (str (second %) (slugify title))
                        :else %)
                 s))

(comment
  (h/html
    (handle-config-markup-directives
      [:->raw "var<&>="
       [:->json {:site "http://timb.net&42"}]
       ])))