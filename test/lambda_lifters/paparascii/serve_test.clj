(ns lambda-lifters.paparascii.serve-test
  (:require [clojure.test :refer [deftest is testing]]
            [lambda-lifters.paparascii.serve :as serve]))

;; NOTE: We test utility functions and content-type detection.
;; Testing the full HTTP server with HttpExchange mocking is overly complex.

(deftest file-extension-test
  (testing "Extracts file extensions correctly"
    (is (= "html" (serve/file-extension "index.html")))
    (is (= "css" (serve/file-extension "style.css")))
    (is (= "js" (serve/file-extension "script.js")))
    (is (= "json" (serve/file-extension "data.json")))
    (is (= "pdf" (serve/file-extension "document.pdf")))
    (is (= "png" (serve/file-extension "image.png")))
    (is (= "jpg" (serve/file-extension "photo.jpg")))
    (is (= "svg" (serve/file-extension "icon.svg")))
    (is (= "woff2" (serve/file-extension "font.woff2")))))

(deftest file-extension-nested-path-test
  (testing "Handles file paths with directories"
    (is (= "html" (serve/file-extension "/path/to/file.html")))
    (is (= "css" (serve/file-extension "assets/css/style.css")))
    (is (= "js" (serve/file-extension "../public/script.js")))))

(deftest file-extension-edge-cases-test
  (testing "Edge cases for file extension extraction"
    (is (nil? (serve/file-extension "no-extension")))
    (is (nil? (serve/file-extension "")))
    (is (= "" (serve/file-extension ".")))
    (is (= "txt" (serve/file-extension ".gitignore.txt")))
    (is (= "" (serve/file-extension "file.")))))

(deftest file-extension-multiple-dots-test
  (testing "Handles files with multiple dots"
    (is (= "gz" (serve/file-extension "archive.tar.gz")))
    (is (= "js" (serve/file-extension "app.min.js")))
    (is (= "html" (serve/file-extension "index.en.html")))))

(deftest content-type-html-test
  (testing "Returns correct content-type for HTML"
    (is (= "text/html" (serve/content-type "index.html")))
    (is (= "text/html" (serve/content-type "/blog/post.html")))))

(deftest content-type-css-js-test
  (testing "Returns correct content-type for CSS and JavaScript"
    (is (= "text/css" (serve/content-type "style.css")))
    (is (= "application/javascript" (serve/content-type "app.js")))))

(deftest content-type-images-test
  (testing "Returns correct content-type for images"
    (is (= "image/png" (serve/content-type "logo.png")))
    (is (= "image/jpeg" (serve/content-type "photo.jpg")))
    (is (= "image/jpeg" (serve/content-type "image.jpeg")))
    (is (= "image/gif" (serve/content-type "animation.gif")))
    (is (= "image/svg+xml" (serve/content-type "icon.svg")))
    (is (= "image/x-icon" (serve/content-type "favicon.ico")))))

(deftest content-type-fonts-test
  (testing "Returns correct content-type for fonts"
    (is (= "font/woff" (serve/content-type "font.woff")))
    (is (= "font/woff2" (serve/content-type "font.woff2")))
    (is (= "font/ttf" (serve/content-type "font.ttf")))
    (is (= "application/vnd.ms-fontobject" (serve/content-type "font.eot")))))

(deftest content-type-other-formats-test
  (testing "Returns correct content-type for other formats"
    (is (= "application/json" (serve/content-type "data.json")))
    (is (= "application/pdf" (serve/content-type "doc.pdf")))))

(deftest content-type-unknown-test
  (testing "Returns default content-type for unknown extensions"
    (is (= "application/octet-stream" (serve/content-type "file.unknown")))
    (is (= "application/octet-stream" (serve/content-type "no-extension")))
    (is (= "application/octet-stream" (serve/content-type "")))))

(deftest content-type-case-sensitivity-test
  (testing "Content-type detection is case-sensitive (uppercase extensions return octet-stream)"
    ;; Extension matching is case-sensitive - only lowercase extensions are mapped
    (is (= "application/octet-stream" (serve/content-type "index.HTML")))
    (is (= "application/octet-stream" (serve/content-type "page.Html")))))
