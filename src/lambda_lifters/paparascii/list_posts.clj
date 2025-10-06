(ns lambda-lifters.paparascii.list-posts
  (:require [clojure.string :as str]
            [clojure.java.io :as io]))

(defn extract-metadata
  "Extract metadata from an AsciiDoc file"
  [file]
  (let [content (slurp file)
        lines (str/split-lines content)
        get-meta (fn [prefix]
                   (some #(when (str/starts-with? % prefix)
                            (str/trim (subs % (count prefix))))
                         lines))]
    {:filename (.getName file)
     :title (get-meta "= ")
     :author (get-meta ":author:")
     :date (get-meta ":date:")
     :description (get-meta ":description:")
     :tags (when-let [tags (get-meta ":tags:")]
             (str/split tags #",\s*"))}))

(defn format-post-info
  "Format post metadata for display"
  [{:keys [filename title author date description tags]}]
  (println (str "\n📄 " filename))
  (when title (println (str "   Title:       " title)))
  (when author (println (str "   Author:      " author)))
  (when date (println (str "   Date:        " date)))
  (when (and description (not (str/blank? description)))
    (println (str "   Description: " description)))
  (when (and tags (seq tags))
    (println (str "   Tags:        " (str/join ", " tags)))))

(defn list-all-posts
  "List all blog posts with their metadata"
  [& {:as _}]
  (let [blog-dir (io/file "blog")
        files (when (.exists blog-dir)
                (filter #(and (.isFile %)
                              (str/ends-with? (.getName %) ".adoc"))
                        (.listFiles blog-dir)))]
    (if (empty? files)
      (do (println "\n📭 No blog posts found in blog/ directory")
          (println "\nCreate your first post with:")
          (println "  bb new-post my-first-post \"My First Post\""))
      (do (println "\n" (str "=== Blog Posts (" (count files) " total) ==="))
          (println (apply str (repeat 40 "=")))
          (let [posts (map extract-metadata files)
                sorted-posts (reverse (sort-by :date posts))]
            (doseq [post sorted-posts]
              (format-post-info post)))
          (println "\n" (apply str (repeat 40 "=")))
          (println "\n💡 Tips:")
          (println "  • Create new post: bb new-post <filename> [\"Title\"]")
          (println "  • Build website:   bb build")
          (println "  • Preview locally: bb serve")))))

(defn -main
  "Main entry point"
  []
  (list-all-posts))
