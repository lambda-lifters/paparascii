(ns lambda-lifters.paparascii.asciidoc.grammar
  "Tools for examining and \"parsing\" asciidoc for text analysis"
  (:require [clojure.string :as str]
            [lambda-lifters.paparascii.util :as u]))

(defn sanitise-passthroughs
  "Remove passthrough HTML content for XSS protection."
  [adoc-source]
  (u/string-replace* adoc-source
                     #"(?s)\+\+\+\+\n.*?\n\+\+\+\+"         ; Remove passthrough blocks (++++...++++)
                     #"pass:\[.*?\]"                        ; Remove inline passthrough (pass:[...])
                     ))

(def header-line? #(str/starts-with? % "="))

(def title-block-line?
  "title block line (starts with \\=) and any leading blank/comment lines"
  #(or (str/blank? %) (str/starts-with? % "=") (str/starts-with? % "//")))

(def attributes-block-line?
  "attribute block lines (start with \\:) and trailing blank lines"
  #(or (str/blank? %) (str/starts-with? % ":")))

(def preamble-block-line?
  "preamble lines (next section heading starting with \\=)"
  #(not (header-line? %)))

(defn extract-preamble-adoc
  "Extracts the preamble paragraph (useful for index cards)"
  [adoc joiner]
  (let [result (->> (str/split-lines adoc)
                    (drop-while title-block-line?)
                    (drop-while attributes-block-line?)
                    (take-while preamble-block-line?)
                    (str/join joiner))]
    (when-not (str/blank? result) result)))

(defn quick-extract-metadata
  "Extract metadata from an AsciiDoc file"
  [content]
  (let [lines (str/split-lines content)
        get-meta (fn [prefix]
                   (some #(when (str/starts-with? % prefix)
                            (str/trim (subs % (count prefix))))
                         lines))]
    {:title       (get-meta "= ")
     :author      (get-meta ":author:")
     :date        (get-meta ":date:")
     :description (get-meta ":description:")
     :tags        (when-let [tags (get-meta ":tags:")]
                    (str/split tags #",\s*"))}))
