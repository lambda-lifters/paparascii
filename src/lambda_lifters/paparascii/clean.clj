(ns lambda-lifters.paparascii.clean
  (:require [lambda-lifters.paparascii.log :as log]
            [lambda-lifters.paparascii.site :as site]))

(defn clean-target!
  "Clean the TARGET directory completely"
  [& {:as _}]
  (log/in-section
    "Cleaning TARGET directory..."
    (let [target (site/target-file)]
      (when (.exists target)
        (doseq [file (file-seq target)
                :when (.isFile file)]
          (.delete file))
        (doseq [dir (reverse (file-seq target))
                :when (.isDirectory dir)]
          (.delete dir))))))
