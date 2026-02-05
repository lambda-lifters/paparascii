(ns lambda-lifters.paparascii.file-patterns
  "Utilities for matching files against patterns using Java NIO glob patterns"
  (:require [clojure.string :as str])
  (:import (java.nio.file FileSystems)))

(defn matches-glob?
  "Test if a string (probably a file name) a glob pattern.
  Uses Java NIO PathMatcher."
  [filename glob]
  (let [fs (FileSystems/getDefault)
        matcher (.getPathMatcher fs (str "glob:" glob))
        path (.getPath fs filename (into-array String []))]
    (.matches matcher path)))

(defn matches-any-glob?
  "Test if a string (probably file name) matches any of the provided globs."
  [filename globs]
  (and (seq globs) (some #(matches-glob? filename %) globs)))

(defn filename-from-path
  "Extract just the filename from a file path."
  [path]
  (last (str/split path #"[/\\\\]")))

(defn path-matches-any-glob?
  "Determine if the file identified by path matches one of the provided glob patterns."
  [file-path globs]
  (matches-any-glob? (filename-from-path file-path) globs))
