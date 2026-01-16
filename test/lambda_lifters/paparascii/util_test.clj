(ns lambda-lifters.paparascii.util-test
  (:require [clojure.test :refer [deftest is testing]]
            [lambda-lifters.paparascii.util :as util]))

(deftest slugify-basic-test
  (testing "Basic slugification"
    (is (= "hello-world" (util/slugify "Hello World")))
    (is (= "hello-world" (util/slugify "hello world")))
    (is (= "hello-world" (util/slugify "HELLO WORLD")))))

(deftest slugify-special-chars-test
  (testing "Removes special characters"
    (is (= "hello-world" (util/slugify "Hello, World!")))
    (is (= "hello-world" (util/slugify "Hello! @#$% World?")))
    (is (= "hello-world" (util/slugify "Hello & World")))
    (is (= "test-123" (util/slugify "Test 123")))
    (is (= "hello_world" (util/slugify "hello_world"))) ; underscores are kept
    (is (= "pre-existing-dash" (util/slugify "pre-existing-dash")))))

(deftest slugify-multiple-spaces-test
  (testing "Collapses multiple spaces to single hyphen"
    (is (= "hello-world" (util/slugify "Hello    World")))
    (is (= "hello-world" (util/slugify "Hello     World")))
    (is (= "a-b-c" (util/slugify "a  b   c")))))

(deftest slugify-multiple-hyphens-test
  (testing "Collapses multiple hyphens to single hyphen"
    (is (= "hello-world" (util/slugify "hello---world")))
    (is (= "hello-world" (util/slugify "hello------world")))
    (is (= "a-b-c" (util/slugify "a--b---c")))))

(deftest slugify-leading-trailing-test
  (testing "Removes leading and trailing hyphens"
    (is (= "hello" (util/slugify "-hello")))
    (is (= "hello" (util/slugify "hello-")))
    (is (= "hello" (util/slugify "-hello-")))
    (is (= "hello" (util/slugify "---hello---")))
    (is (= "hello-world" (util/slugify "  hello world  ")))))

(deftest ^:kaocha/skip slugify-unicode-conversion-test
  (testing "Converts unicode characters to text representations (NOT YET IMPLEMENTED)"
    ;; Future enhancement: convert emoji and unicode to readable text
    ;; e.g., ❤️ -> "heart", 💙 -> "blue-heart", etc.
    (is (= "heart-blue-heart-green-heart" (util/slugify "❤️💙💚")))
    (is (= "hello-world" (util/slugify "Héllo Wörld")))
    (is (= "test-test" (util/slugify "测试 test")))))

(deftest slugify-unicode-current-behavior-test
  (testing "Current behavior: removes unicode characters that aren't word chars"
    ;; Current implementation removes non-ASCII word chars
    (is (= "hello-world" (util/slugify "Héllo Wörld")))
    (is (= "test" (util/slugify "测试 test")))
    (is (= "" (util/slugify "❤️💙💚")))))

(deftest slugify-edge-cases-test
  (testing "Edge cases"
    (is (= "" (util/slugify "")))
    (is (= "" (util/slugify "   ")))
    (is (= "" (util/slugify "!@#$%^&*()")))
    (is (= "" (util/slugify "---")))
    (is (= "a" (util/slugify "a")))
    (is (= "123" (util/slugify "123")))))

(deftest slugify-mixed-content-test
  (testing "Mixed content with spaces, special chars, and hyphens"
    (is (= "my-awesome-blog-post" (util/slugify "My Awesome Blog Post!!!")))
    (is (= "2024-guide-to-clojure" (util/slugify "2024 Guide to Clojure")))
    (is (= "hello-world-2024" (util/slugify "  Hello, World! (2024)  ")))
    (is (= "test_with_underscores" (util/slugify "test_with_underscores")))))

(deftest slugify-already-slugified-test
  (testing "Already slugified strings remain unchanged"
    (is (= "already-slugified" (util/slugify "already-slugified")))
    (is (= "test123" (util/slugify "test123")))
    (is (= "my_slug_123" (util/slugify "my_slug_123")))))
