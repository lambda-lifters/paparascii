(ns lambda-lifters.paparascii.adoc
  (:require [clojure.string :as str]
            [lambda-lifters.paparascii.prism-js-highlighter :as highlighter])
  (:import (org.asciidoctor Asciidoctor$Factory Attributes Options SafeMode)))

;; Create a singleton Asciidoctor instance
(def *asciidoctor (delay (let [doctor (Asciidoctor$Factory/create)]
                           (highlighter/register-highlighter-with-doctor doctor)
                           doctor)))

(def *asciidoctor-options
  (delay
    (let [attributes
          (-> (Attributes/builder)
              (.backend "html5")
              (.showTitle false)                            ; Don't show title (we handle it)
              (.noFooter true)
              (.linkCss true)
              (.copyCss true)
              (.sourceHighlighter highlighter/highlighter-name)
              (.attribute "sectids" false)
              .build)
          options (-> (Options/builder)
                      (.safe SafeMode/SAFE)
                      (.toFile false)
                      (.standalone false)                   ; No header/footer
                      (.attributes attributes))]
      (.build options))))

(defn asciidoc-to-html
  "Convert AsciiDoc to HTML using AsciidoctorJ"
  [content & _]
  (.convert @*asciidoctor content @*asciidoctor-options))

(defn parse-asciidoc-header
  "Extract metadata from AsciiDoc header"
  [content]
  (let [lines (str/split-lines content)
        get-meta (fn [prefix]
                   (some #(when (str/starts-with? % prefix)
                            (str/trim (subs % (count prefix))))
                         lines))]
    {:title       (or (get-meta "= ") "Untitled")
     :author      (or (get-meta ":author:") "Anonymous")
     :date        (get-meta ":date:")
     :description (get-meta ":description:")
     :tags        (when-let [tags (get-meta ":tags:")] (str/split tags #",\s*"))}))

