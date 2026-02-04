(ns lambda-lifters.paparascii.util
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (java.text Normalizer Normalizer$Form)))

(defn string-replace* [s & re-replacement-tuples]
  (reduce #(if (vector? %2)
             (str/replace %1 (first %2) (or (second %2) ""))
             (str/replace %1 %2 ""))
          s
          re-replacement-tuples))

(defn slugify
  "Convert a title to a URL- (and file-) friendly slug"
  [title]
  (-> title
      str/lower-case
      (Normalizer/normalize Normalizer$Form/NFD)            ; Decompose accented chars (ü → u + combining marks)
      (string-replace*
       #"\p{M}+"                                           ; Strip all combining marks (accents, umlauts, etc.)
       #"[^\w \s-]"                                        ; Remove non-word chars except spaces and hyphens
       [#"\s+" "-"]                                        ; Replace spaces with hyphens
       [#"-+" "-"]                                         ; Replace multiple hyphens with single
       #"^-|-$")))                                         ; Remove leading/trailing hyphens

(defn slugify-file
  "Converts the basename of a file name to a slug"
  [file]
  (-> file io/file .getName (str/replace #"\.[^.]*$" "") slugify))