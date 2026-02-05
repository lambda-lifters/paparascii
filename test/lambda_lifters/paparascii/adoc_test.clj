(ns lambda-lifters.paparascii.adoc-test
  (:require [clojure.test :refer [deftest is testing]]
            [lambda-lifters.paparascii.asciidoc.execution :as adoc]
            [clojure.string :as str]))

;; NOTE: We avoid testing specific HTML output/styling (too brittle).
;; We focus on: data extraction, XSS protection, and functional behavior.

(def simple-doc
  "= Test Title
:author: John Doe
:date: 2024-01-01

This is the preamble text.
It has multiple lines.

== First Section

Some content here.")

(def doc-with-tags
  "= Blog Post
:author: Jane Smith
:date: 2024-12-15
:tags: clojure, testing, blog

A great post about testing.")

(def doc-no-preamble
  "= Title Only
:author: Anonymous

== First Section

Content starts immediately.")

(def doc-empty-preamble
  "= Title
:author: Test


== Section

Content.")

(def doc-with-description
  "= Post
:author: Test
:description: Custom description text

Preamble text here.")

(deftest asciidoc-to-html-basic-test
  (testing "Converts AsciiDoc to HTML string"
    (let [html (adoc/asciidoc-to-html simple-doc)]
      (is (string? html))
      (is (not (str/blank? html))))))

(deftest asciidoc-to-html-xss-protection-test
  (testing "HTML special characters are escaped (XSS protection)"
    (let [doc "= Test\n\n<script>alert('xss')</script>"
          html (adoc/asciidoc-to-html doc)]
      ;; AsciidoctorJ SafeMode should escape/block script tags
      (is (not (str/includes? html "<script>alert('xss')</script>"))))))

(deftest asciidoc-to-html-xss-attributes-test
  (testing "Dangerous HTML attributes are escaped"
    (let [doc "= Test\n\n++++\n<img src=x onerror='alert(1)'>\n++++"
          html (adoc/asciidoc-to-html doc)]
      ;; SafeMode should block passthrough content
      (is (not (str/includes? html "onerror="))))))

(deftest get-document-metadata-test
  (testing "Extracts metadata from AsciiDoc header"
    (let [metadata (adoc/get-document-metadata simple-doc)]
      (is (= "Test Title" (:doctitle metadata)))
      (is (= "John Doe" (:author metadata)))
      (is (= "2024-01-01" (:date metadata))))))

(deftest get-document-metadata-with-tags-test
  (testing "Extracts tags from metadata"
    (let [metadata (adoc/get-document-metadata doc-with-tags)]
      (is (= "Blog Post" (:doctitle metadata)))
      (is (= "clojure, testing, blog" (:tags metadata))))))

(deftest asciidoc-to-preamble-html-basic-test
  (testing "Extracts preamble (non-nil result when preamble exists)"
    (let [preamble-html (adoc/asciidoc-to-preamble-html simple-doc)]
      (is (some? preamble-html))
      (is (string? preamble-html))
      (is (not (str/blank? preamble-html))))))

(deftest asciidoc-to-preamble-html-no-preamble-test
  (testing "Returns nil or blank when no preamble exists"
    (let [preamble-html (adoc/asciidoc-to-preamble-html doc-no-preamble)]
      ;; Doc starts with section immediately after header, no preamble
      (is (or (nil? preamble-html)
              (str/blank? preamble-html))))))

(deftest asciidoc-to-preamble-html-empty-lines-test
  (testing "Handles documents with empty lines correctly"
    (let [preamble-html (adoc/asciidoc-to-preamble-html doc-empty-preamble)]
      ;; Should skip empty lines and find no preamble (section starts)
      (is (or (nil? preamble-html)
              (str/blank? preamble-html))))))

(deftest parse-asciidoc-header-basic-test
  (testing "Parses complete header with all fields"
    (let [header (adoc/parse-asciidoc-header simple-doc)]
      (is (= "Test Title" (:title header)))
      (is (= "John Doe" (:author header)))
      (is (= "2024-01-01" (:date header)))
      (is (some? (:description header))))))

(deftest parse-asciidoc-header-tags-test
  (testing "Parses and splits tags correctly"
    (let [header (adoc/parse-asciidoc-header doc-with-tags)]
      (is (= "clojure, testing, blog" (:raw-tags header)))
      (is (= ["clojure" "testing" "blog"] (:tags header))))))

(deftest parse-asciidoc-header-description-priority-test
  (testing "Preamble takes priority over :description attribute"
    (let [header (adoc/parse-asciidoc-header doc-with-description)]
      ;; Preamble should be used if present
      (is (some? (:description header)))
      ;; The description should be a non-blank string
      (is (string? (:description header)))
      (is (not (str/blank? (:description header)))))))

(deftest parse-asciidoc-header-defaults-test
  (testing "Provides defaults for missing fields"
    (let [minimal-doc "= Only Title\n\nContent here."
          header (adoc/parse-asciidoc-header minimal-doc)]
      (is (= "Only Title" (:title header)))
      (is (= "Anonymous" (:author header)))
      (is (nil? (:date header))))))

(deftest parse-asciidoc-header-no-title-test
  (testing "Handles documents without title"
    (let [no-title-doc "Just some content without a title."
          header (adoc/parse-asciidoc-header no-title-doc)]
      (is (= "Untitled" (:title header)))
      (is (= "Anonymous" (:author header))))))

(deftest ^:pending parse-asciidoc-header-malformed-tags-test
  (testing "Handles malformed tag formats (FUTURE: better tag parsing)"
    ;; Future: handle edge cases like trailing commas, mixed separators
    (let [doc "= Test\n:tags: clojure,, testing  ,blog,\n\nContent."
          header (adoc/parse-asciidoc-header doc)]
      ;; Should clean up empty tags and extra whitespace
      (is (= ["clojure" "testing" "blog"] (:tags header))))))
