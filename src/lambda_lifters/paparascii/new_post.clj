(ns lambda-lifters.paparascii.new-post
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [lambda-lifters.paparascii.util :as u]
            [lambda-lifters.lambda-liftoff.environmental :as e])
  (:import (java.time LocalDate)
           (java.time.format DateTimeFormatter)))

(defn load-template
  "Load a template file and replace placeholders"
  [template-path replacements]
  (let [template (slurp template-path)]
    (reduce (fn [content [k v]]
              (str/replace content (str "{{" (name k) "}}") (str v)))
            template
            replacements)))

(defn create-post
  "Create a new blog post with the given title"
  [& {:keys [title]}]
  (if-not title
    (log/error "Error: Please provide a title with :title \"Your Title\"")
    (let [author (e/get-git-user-name)
          date (.format (LocalDate/now)
                        DateTimeFormatter/ISO_LOCAL_DATE)
          filename (str "blog/" (u/slugify title) ".adoc")
          ;; Load template and replace placeholders
          content (load-template "templates/new-post.adoc"
                                 {:title       title
                                  :author      author
                                  :date        date
                                  :description "Brief description of your post"
                                  :tags        "tag1, tag2"})]

      (if (.exists (io/file filename))
        (do
          (log/error "❌ File already exists: " filename " Please choose a different title or delete the existing file."))
        (do
          (io/make-parents filename)
          (spit filename content)
          (log/info "
          ✅ Created new blog post: " filename "
          Next steps:
            1. Edit the file: " filename "
            2. Build the site: clojure -T:build build
            3. Test locally: clojure -T:build serve"))))))
