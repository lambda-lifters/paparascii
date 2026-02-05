(ns lambda-lifters.paparascii.clean
  (:require [clojure.tools.logging :as log]
            [lambda-lifters.paparascii.site :as site])
  (:import (java.io File)))

(defn- clean-target-files! [target]
  (dorun
    (->> target
         file-seq
         (filter File/.isFile)
         (map File/.delete))))

(defn- clean-target-dirs! [target]
  (dorun
    (->> target
         file-seq
         reverse
         (filter File/.isDirectory)
         (map File/.delete))))

(defn clean-target!
  "Clean the TARGET directory completely"
  [& {:as _}]
  (log/info "Cleaning TARGET directory...")
  (let [target (site/target-file)]
    (when (.exists target)
      (clean-target-files! target)
      (clean-target-dirs! target))))
