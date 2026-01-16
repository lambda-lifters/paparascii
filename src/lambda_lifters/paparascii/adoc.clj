(ns lambda-lifters.paparascii.adoc
  (:require [clojure.string :as str]
            [lambda-lifters.paparascii.prism-js-highlighter :as highlighter])
  (:import (java.util Map)
           (org.asciidoctor Asciidoctor Asciidoctor$Factory Attributes Options SafeMode)
           (org.asciidoctor.ast Document)))

;; Create a resettable-singleton Asciidoctor instance
(defonce *asciidoctor (atom nil))

(defn the-doctor [& {:keys [reset?]}]
  (when reset? (reset! *asciidoctor nil))
  (swap! *asciidoctor #(or %
                           (let [doctor (Asciidoctor$Factory/create)]
                             (highlighter/register-highlighter-with-doctor doctor)
                             doctor))))

(defn make-attributes [highlighter-name allow-passthroughs?]
  (cond-> {
           Attributes/BACKEND            "html5"
           Attributes/SHOW_TITLE         false              ; Don't show title (we handle it)
           Attributes/NO_FOOTER          true
           Attributes/LINK_CSS           true
           Attributes/COPY_CSS           true
           Attributes/ICONS              "font"             ; Enable Font Awesome icons
           Attributes/SOURCE_HIGHLIGHTER highlighter-name
           "sectids"                     false}
          ;; Disable passthroughs unless explicitly allowed
          (not allow-passthroughs?) (merge {Attributes/SKIP_FRONT_MATTER true
                                            Attributes/ALLOW_URI_READ    false})))


(defn- build-asciidoctor-options
  "Build AsciidoctorJ options based on site configuration.

   Parameters:
     site-config - Site configuration map (optional)

   Security:
     Disables passthrough content (++++, pass:[]) by default for XSS protection.
     If :asciidoc-allow-passthroughs? is true, passthroughs are allowed."
  [site-config]
  (let [allow-passthroughs? (get site-config :asciidoc-allow-passthroughs? false)
        safe-mode SafeMode/SECURE
        attributes (doto (.build (Attributes/builder))
                     (^[Map] Attributes/.setAttributes
                       (make-attributes highlighter/highlighter-name allow-passthroughs?)))
        options (-> (Options/builder)
                    (.safe safe-mode)
                    (.toFile false)
                    (.standalone false)                     ; No header/footer
                    (.attributes attributes))]
    (.build options)))

(defn- sanitize-html
  "Remove passthrough HTML content for XSS protection.
   Removes content between ++++ delimiters before processing."
  [content]
  (-> content
      ;; Remove passthrough blocks (++++...++++)
      (str/replace #"(?s)\+\+\+\+\n.*?\n\+\+\+\+" "")
      ;; Remove inline passthrough (pass:[...])
      (str/replace #"pass:\[.*?\]" "")))

(defn asciidoc-to-html
  "Convert AsciiDoc to HTML using AsciidoctorJ

   Parameters:
     content - AsciiDoc content string
     file - File path (optional, for context)
     site-config - Site configuration map (optional, for security settings)"
  [content & [file site-config]]
  (let [allow-passthroughs? (get site-config :asciidoc-allow-passthroughs? false)
        ;; Strip passthrough blocks if not allowed
        content (if allow-passthroughs? content (sanitize-html content))
        options (build-asciidoctor-options site-config)]
    (.convert (the-doctor) content options)))

(defn get-document-metadata
  "Extract metadata attributes from AsciiDoc document

   Parameters:
     content - AsciiDoc content string
     file - File path (optional, for context)
     site-config - Site configuration map (optional, for security settings)"
  [content & [file site-config]]
  (let [options (build-asciidoctor-options site-config)]
    (into {}
          (map (juxt (comp keyword clojure.string/lower-case key) val))
          (Document/.getAttributes
            (Asciidoctor/.load (the-doctor) content options)))))

(defn asciidoc-to-preamble-html
  "Converts only the preamble to HTML

   Parameters:
     content - AsciiDoc content string
     file - File path (optional, for context)
     site-config - Site configuration map (optional, for security settings)"
  [content & [file site-config]]
  (let [lines (str/split-lines content)
        ;; Skip title line (starts with "=") and any leading blank/comment lines
        after-title (drop-while #(or (str/blank? %)
                                     (str/starts-with? % "=")
                                     (str/starts-with? % "//"))
                                lines)
        ;; Skip attribute lines (start with ":") and trailing blank lines
        after-attrs (drop-while #(or (str/starts-with? % ":")
                                     (str/blank? %))
                                after-title)
        ;; Take preamble lines (until next section heading starting with "=")
        preamble-lines (take-while #(not (str/starts-with? % "=")) after-attrs)
        preamble-text (str/join "\n" preamble-lines)]
    (when-not (str/blank? preamble-text)
      (asciidoc-to-html preamble-text file site-config))))

(defn asciidoc-preamble-to-text
  "Extract plain text from the preamble (first paragraph) without HTML wrapping"
  [content]
  (let [lines (str/split-lines content)
        ;; Skip to first blank line (end of header section)
        ;; Header includes: title, author, revision, attributes
        after-header (->> lines
                          (drop-while #(not (str/blank? %))) ; Find first blank line
                          (drop-while str/blank?))          ; Skip any additional blanks
        ;; Take first paragraph (until blank line or next section)
        first-para-lines (take-while #(and (not (str/blank? %))
                                           (not (str/starts-with? % "=")))
                                     after-header)
        ;; Join lines with spaces (paragraphs in AsciiDoc are continuous)
        first-para (str/join " " first-para-lines)]
    (when-not (str/blank? first-para)
      first-para)))

(defn parse-asciidoc-header
  "Extract metadata from AsciiDoc header

   Parameters:
     content - AsciiDoc content string
     file - File path (optional, for context)
     site-config - Site configuration map (optional, for security settings)"
  [content & [file site-config]]
  (let [document-metadata (get-document-metadata content file site-config)
        allow-passthroughs? (get site-config :asciidoc-allow-passthroughs? false)]
    {:title       (get document-metadata :doctitle "Untitled")
     :author      (get document-metadata :author "Anonymous")
     :date        (:date document-metadata)
     :description (or (:description document-metadata)
                      (if allow-passthroughs?
                        (asciidoc-to-preamble-html content file site-config)
                        (asciidoc-preamble-to-text content)))
     :raw-tags    (:tags document-metadata)
     :tags        (some-> document-metadata :tags (str/split #"(,|\s)+"))}))
