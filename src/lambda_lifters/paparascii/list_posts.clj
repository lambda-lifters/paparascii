(ns lambda-lifters.paparascii.list-posts
  (:require
    [lambda-lifters.paparascii.asciidoc.grammar :as g]
    [lambda-lifters.paparascii.asciidoc.grammar as :g]
    [clojure.java.io :as io]
    [clojure.string :as str]))

(defn extract-metadata
  "Extract metadata from an AsciiDoc file"
  [file]
  (assoc (g/quick-extract-metadata (slurp file))
    :filename (.getName file)))

(defn format-post-info
  "Format post metadata for display"
  [{:keys [filename title author date description tags]}]
  (dorun
    (->> [(str "📄 " filename)
          (when title (str "   Title:       " title))
          (when author (str "   Author:      " author))
          (when date (str "   Date:        " date))
          (when (and description (not (str/blank? description)))
            (str "   Description: " description))
          (when (and tags (seq tags))
            (str "   Tags:        " (str/join ", " tags)))]
         (filter identity)
         (str/join "\n"))))

(defn list-all-posts
  "List all blog posts with their metadata"
  [& {:as _}]
  (let [blog-dir (io/file "blog")
        files (when (.exists blog-dir)
                (filter #(and (.isFile %)
                              (str/ends-with? (.getName %) ".adoc"))
                        (.listFiles blog-dir)))]
    (if (empty? files)
      (do (println "📭 No blog posts found in blog/ directory")
          (println "Create your first post with:")
          (println "  bb new-post my-first-post \"My First Post\""))
      (do (println (str "=== Blog Posts (" (count files) " total) ==="))
          (println (apply str (repeat 40 "=")))
          (->> files
             (map extract-metadata)
             (sort-by :date)
             reverse
             (map format-post-info)
             (str/join "\n")
             println)
          (println (apply str (repeat 40 "=")))))))

(defn -main
  "Main entry point"
  []
  (list-all-posts))
