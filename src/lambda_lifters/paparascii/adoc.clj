(ns lambda-lifters.paparascii.adoc
  (:require [clojure.string :as str]
            [lambda-lifters.paparascii.prism-js-highlighter :as highlighter])
  (:import (org.asciidoctor Asciidoctor Asciidoctor$Factory Attributes Options SafeMode)
           (org.asciidoctor.ast Document)))

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

(defn get-document-metadata [content & _]
  (into {}
        (map (juxt (comp keyword clojure.string/lower-case key) val))
        (Document/.getAttributes
          (Asciidoctor/.load @*asciidoctor content @*asciidoctor-options))))

(defn parse-asciidoc-header
  "Extract metadata from AsciiDoc header"
  [content]
  (let [document-metadata (get-document-metadata content)]
    {:title       (get document-metadata :doctitle "Untitled")
     :author      (get document-metadata :author "Anonymous")
     :date        (:date document-metadata)
     :description (:description document-metadata)
     :raw-tags    (:tags document-metadata)
     :tags        (some-> document-metadata :tags (str/split #"(,|\s)+"))}))
