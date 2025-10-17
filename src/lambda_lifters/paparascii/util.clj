(ns lambda-lifters.paparascii.util
  (:require [clojure.string :as str]))

(defn slugify
  "Convert a title to a URL- (and file-) friendly slug"
  [title]
  (-> title
      str/lower-case
      (str/replace #"[^\w \s-]" "")                         ; Remove non-word chars except spaces and hyphens
      (str/replace #"\s+" "-")                              ; Replace spaces with hyphens
      (str/replace #"-+" "-")                               ; Replace multiple hyphens with single
      (str/replace #"^-|-$" "")))                           ; Remove leading/trailing hyphens