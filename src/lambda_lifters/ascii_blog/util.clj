(ns lambda-lifters.ascii-blog.util
  (:require [lambda-lifters.ascii-blog.log :as log]
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
  (io/make-parents dest)
  (io/copy (io/file src)
           (io/file dest))
  (log/success "  ✓ copied " (.toString dest)))

(defn ensure-directory [dir]
  (io/make-parents (io/file dir "dummy"))
  (.delete (io/file dir "dummy"))
  (log/success "  ✓ Created " dir))

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