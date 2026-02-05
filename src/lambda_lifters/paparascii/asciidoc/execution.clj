(ns lambda-lifters.paparascii.asciidoc.execution
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [lambda-lifters.paparascii.asciidoc.grammar :as g]
            [lambda-lifters.paparascii.prism-js-highlighter :as highlighter])
  (:import (java.util Map)
           (org.asciidoctor Asciidoctor Asciidoctor$Factory Attributes Options SafeMode)
           (org.asciidoctor.ast Document)))

;; Create a resettable-singleton Asciidoctor instance
(defonce *asciidoctor (atom nil))

(defn the-doctor [& {:keys [reset?]}]
  (when reset? (reset! *asciidoctor nil))
  (swap! *asciidoctor #(or %
                           (log/info "Fabricating Asciidoctor")
                           (doto (Asciidoctor$Factory/create)
                             highlighter/register-highlighter-with-doctor))))

(def ^:private default-attributes
  {Attributes/BACKEND    "html5"
   Attributes/SHOW_TITLE false                              ; Don't show title (we handle it)
   Attributes/NO_FOOTER  true
   Attributes/LINK_CSS   true
   Attributes/COPY_CSS   true
   Attributes/ICONS      "font"                             ; Enable Font Awesome icons
   "sectids"             false})

(defn- make-attributes [highlighter-name allow-passthroughs?]
  (cond-> default-attributes
          highlighter-name (assoc Attributes/SOURCE_HIGHLIGHTER highlighter-name)
          ;; Disable passthroughs unless explicitly allowed
          (not allow-passthroughs?) (merge {Attributes/SKIP_FRONT_MATTER true
                                            Attributes/ALLOW_URI_READ    false})))

(defn- build-asciidoctor-options
  "Build AsciidoctorJ options based on site configuration.
   Security:
     Disables passthrough content (++++, pass:[]) by default for XSS protection.
     If :asciidoc-allow-passthroughs? is true, passthroughs are allowed."
  [allow-passthroughs?]
  (let [attributes (doto (.build (Attributes/builder))
                     (^[Map] Attributes/.setAttributes
                       (make-attributes highlighter/highlighter-name allow-passthroughs?)))
        options (-> (Options/builder)
                    (.safe SafeMode/SECURE)
                    (.toFile false)
                    (.standalone false)                     ; No header/footer
                    (.attributes attributes))]
    (.build options)))

(defn asciidoc-to-html
  "Convert AsciiDoc to HTML using AsciidoctorJ
   Parameters:
     file - File path (optional, for context)
     site-config - Site configuration map"
  [adoc-source & {:keys [asciidoc-allow-passthroughs?]}]
  (let [adoc-source (if asciidoc-allow-passthroughs? adoc-source (g/sanitise-passthroughs adoc-source))
        options (build-asciidoctor-options asciidoc-allow-passthroughs?)]
    (.convert (the-doctor) adoc-source options)))

(defn get-document-metadata
  "Extract metadata attributes from AsciiDoc document
   Parameters:
     file - File path (optional, for context)
     site-config - Site configuration map (optional, for security settings)"
  [adoc-source & [site-config]]
  (let [options (build-asciidoctor-options site-config)]
    (into {}
          (map (juxt (comp keyword clojure.string/lower-case key) val))
          (Document/.getAttributes
            (Asciidoctor/.load (the-doctor) adoc-source options)))))

(defn asciidoc-to-preamble-html
  "Converts only the preamble to HTML
   Parameters:
     file - File path (optional, for context)
     site-config - Site configuration map (optional, for security settings)"
  [adoc-source & [file site-config]]
  (some-> adoc-source
          (g/extract-preamble-adoc "\n")
          (asciidoc-to-html file site-config)))

(defn asciidoc-preamble-to-text
  "Extract plain text from the preamble (first paragraph) without HTML wrapping"
  [adoc-source]
  (g/extract-preamble-adoc adoc-source " "))

(defn parse-asciidoc-header
  "Extract metadata from AsciiDoc header
   Parameters:
     file - File path (optional, for context)
     site-config - Site configuration map (optional, for security settings)"
  [adoc-source & [file site-config]]
  (let [document-metadata (get-document-metadata adoc-source file site-config)
        allow-passthroughs? (:asciidoc-allow-passthroughs? site-config false)]
    {:title       (:doctitle document-metadata "Untitled")
     :author      (:author document-metadata "Anonymous")
     :date        (:date document-metadata)
     :description (or (:description document-metadata)
                      (if allow-passthroughs?
                        (asciidoc-to-preamble-html adoc-source file site-config)
                        (asciidoc-preamble-to-text adoc-source)))
     :raw-tags    (:tags document-metadata)
     :tags        (some-> document-metadata :tags (str/split #"(,|\s)+"))}))
