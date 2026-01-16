(ns lambda-lifters.paparascii.path-security
  "Security utilities for preventing path traversal attacks"
  (:require [clojure.java.io :as io])
  (:import (java.io File)))

(defn canonical-path
  "Get canonical path of a file, handling IOException.
  Canonical paths are absolute and resolve all symbolic links and . / .. segments."
  [file]
  (.getCanonicalPath (io/as-file file)))

(defn path-within-root?
  "Check if file path is within root directory.
  Returns true if file-path is exactly root-path or starts with root-path followed by separator."
  [root-path file-path]
  (let [root-normalized (if (or (.endsWith root-path "/") (.endsWith root-path "\\"))
                          root-path
                          (str root-path File/separator))]
    (or (= file-path root-path)
        (.startsWith file-path root-normalized))))

(defn validate-path!
  "Validate that requested-file is within root-dir.
  Throws SecurityException if path traversal is detected.
  Returns the canonical path of the requested file if valid."
  [root-dir requested-file]
  (let [root-canonical (canonical-path root-dir)
        file-canonical (canonical-path requested-file)]
    (when-not (path-within-root? root-canonical file-canonical)
      (throw (SecurityException. "Path traversal attempt detected")))
    file-canonical))

(defn detect-suspicious-patterns
  "Check for suspicious patterns in path string that might indicate traversal attempts.
  Throws SecurityException if suspicious patterns are detected."
  [path]
  (let [path-str (if (instance? File path) (.getPath ^File path) (str path))]
    ;; Normalize separators to forward slashes for consistent checking
    (let [normalized (.replace path-str "\\" "/")]
      ;; Check for parent directory references
      (when (or (.contains normalized "../")
                (.contains normalized "..")
                ;; Check for multiple dots that might be obfuscation attempts
                (re-find #"\.\.\.\." normalized))
        (throw (SecurityException. "Suspicious path pattern detected"))))))

(defn safe-file
  "Create a File object safely by validating it's within root-dir.
  Throws SecurityException if path traversal is detected.
  Returns the validated File object."
  [root-dir path]
  ;; First check for suspicious patterns in the input path
  (detect-suspicious-patterns path)
  (let [file (io/file root-dir path)]
    (validate-path! root-dir file)
    file))
