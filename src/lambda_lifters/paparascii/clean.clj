(ns lambda-lifters.paparascii.clean
  (:require [clojure.tools.logging :as log]
            [lambda-lifters.paparascii.site :as site])
  (:import (java.io File)))

(defn clean-target!
  "Clean the TARGET directory completely"
  [& {:as _}]
  (log/info "Cleaning TARGET directory...")
  (let [target (site/target-file)]
    (when (.exists target)
      (->> target file-seq (filter File/.isFile) (map File/.delete) dorun)
      (->> target file-seq reverse (filter File/.isDirectory) (map File/.delete) dorun))))
