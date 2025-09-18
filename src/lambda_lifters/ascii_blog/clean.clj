(ns lambda-lifters.ascii-blog.clean
  (:require [lambda-lifters.ascii-blog.log :as log]
            [lambda-lifters.ascii-blog.site :as site]))

(defn clean-target!
  "Clean the TARGET directory completely"
  [& {:as _}]
  (log/section "Cleaning TARGET directory...")
  (let [target (site/target-file)]
    (when (.exists target)
      (doseq [file (file-seq target)
              :when (.isFile file)]
        (.delete file))
      (doseq [dir (reverse (file-seq target))
              :when (.isDirectory dir)]
        (.delete dir))))
  (log/success "  ✓ TARGET cleaned"))

