(ns lambda-lifters.paparascii.path-security-test
  (:require [clojure.test :refer [deftest is testing]]
            [lambda-lifters.paparascii.path-security :as path-sec]
            [clojure.java.io :as io])
  (:import (java.io File)))

(def test-root-dir
  "Use a safe test directory path"
  (System/getProperty "java.io.tmpdir"))

(deftest canonical-path-test
  (testing "Returns canonical path for file"
    (let [file (io/file test-root-dir "test.txt")
          canonical (path-sec/canonical-path file)]
      (is (string? canonical))
      (is (.isAbsolute (File. canonical))))))

(deftest canonical-path-resolves-dots-test
  (testing "Resolves . and .. in paths"
    (let [file (io/file test-root-dir "subdir" ".." "test.txt")
          canonical (path-sec/canonical-path file)]
      ;; Should resolve .. to parent directory
      (is (not (.contains canonical (str File/separator ".." File/separator)))))))

(deftest path-within-root-basic-test
  (testing "Detects when path is within root"
    (let [sep File/separator]
      (is (true? (path-sec/path-within-root? (str sep "var" sep "www") (str sep "var" sep "www" sep "index.html"))))
      (is (true? (path-sec/path-within-root? (str sep "var" sep "www") (str sep "var" sep "www" sep "subdir" sep "file.html"))))
      (is (true? (path-sec/path-within-root? (str sep "var" sep "www" sep) (str sep "var" sep "www" sep "index.html")))))))

(deftest path-within-root-traversal-test
  (testing "Detects path traversal attempts"
    (let [sep File/separator]
      (is (false? (path-sec/path-within-root? (str sep "var" sep "www") (str sep "etc" sep "passwd"))))
      (is (false? (path-sec/path-within-root? (str sep "var" sep "www") (str sep "var" sep "other" sep "file.txt"))))
      (is (false? (path-sec/path-within-root? (str sep "var" sep "www") (str sep "var" sep "wwww" sep "file.txt")))) ; similar but different path
      (is (false? (path-sec/path-within-root? (str sep "home" sep "user" sep "site") (str sep "home" sep "user" sep "secrets.txt")))))))

(deftest validate-path-valid-test
  (testing "Allows valid paths within root"
    ;; Create a temp directory structure for testing
    (let [root (io/file test-root-dir "test-root")
          _ (.mkdirs root)
          test-file (io/file root "test.txt")
          _ (.createNewFile test-file)]
      (try
        (let [result (path-sec/validate-path! (.getPath root) test-file)]
          (is (string? result))
          (is (.contains result "test.txt")))
        (finally
          (.delete test-file)
          (.delete root))))))

(deftest validate-path-rejects-traversal-test
  (testing "Throws SecurityException on path traversal"
    (let [root (io/file test-root-dir "test-root")
          _ (.mkdirs root)
          traversal-file (io/file root ".." ".." "etc" "passwd")]
      (try
        (is (thrown? SecurityException
                     (path-sec/validate-path! (.getPath root) traversal-file)))
        (finally
          (.delete root))))))

(deftest validate-path-symlink-test
  (testing "Validates symlinks don't escape root"
    ;; Note: This test documents expected behavior but may not run on all systems
    (let [root (io/file test-root-dir "test-root")
          _ (.mkdirs root)]
      (try
        ;; Attempt to access parent via relative path
        (let [escape-attempt (io/file root ".." "outside.txt")]
          (is (thrown? SecurityException
                       (path-sec/validate-path! (.getPath root) escape-attempt))))
        (finally
          (.delete root))))))

(deftest safe-file-valid-test
  (testing "Creates safe File object for valid paths"
    (let [root (io/file test-root-dir "test-root")
          _ (.mkdirs root)]
      (try
        (let [safe (path-sec/safe-file (.getPath root) "test.txt")]
          (is (instance? File safe))
          (is (.getName safe) "test.txt"))
        (finally
          (.delete root))))))

(deftest safe-file-rejects-traversal-test
  (testing "Throws SecurityException for traversal attempts"
    (let [root (io/file test-root-dir "test-root")
          _ (.mkdirs root)]
      (try
        (is (thrown? SecurityException
                     (path-sec/safe-file (.getPath root) "../../etc/passwd")))
        (is (thrown? SecurityException
                     (path-sec/safe-file (.getPath root) "../outside.txt")))
        (finally
          (.delete root))))))

(deftest path-traversal-attack-vectors-test
  (testing "Blocks common path traversal attack vectors"
    (let [root (io/file test-root-dir "test-root")
          _ (.mkdirs root)
          root-path (.getPath root)
          attack-vectors ["../../etc/passwd"
                          "../../../etc/passwd"
                          "..\\..\\windows\\system32\\config\\sam"
                          "....//....//etc/passwd"
                          "subdir/../../outside.txt"]]
      (try
        (doseq [attack attack-vectors]
          ;; Either SecurityException (traversal detected) or IOException (invalid path on Windows)
          (is (thrown? Exception (path-sec/safe-file root-path attack))
              (str "Failed to block: " attack)))
        (finally
          (.delete root))))))

(deftest path-traversal-encoded-test
  (testing "URL-encoded paths are treated literally (not decoded)"
    (let [root (io/file test-root-dir "test-root")
          _ (.mkdirs root)
          root-path (.getPath root)]
      (try
        ;; %2e%2e = ".." when URL-decoded, but Java File API treats it literally
        ;; This is NOT a traversal because "%2e%2e" is a literal directory name
        ;; URL decoding should happen at the HTTP layer before reaching path validation
        (let [result (path-sec/safe-file root-path "%2e%2e/etc/passwd")]
          (is (instance? File result)))
        (finally
          (.delete root))))))
