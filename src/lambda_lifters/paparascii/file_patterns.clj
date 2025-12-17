(ns lambda-lifters.paparascii.file-patterns
  "Utilities for matching files against patterns using Java NIO glob patterns"
  (:import (java.nio.file FileSystems)))

(defn matches-pattern?
  "Check if a filename matches a glob pattern using Java NIO PathMatcher.

  Parameters:
    filename - the filename to check (e.g., 'script.sh')
    pattern  - the glob pattern (e.g., '*.sh', 'my-script', '*')

  Returns:
    true if the filename matches the pattern, false otherwise

  Examples:
    (matches-pattern? \"script.sh\" \"*.sh\")     => true
    (matches-pattern? \"test.cgi\" \"*.sh\")      => false
    (matches-pattern? \"my-script\" \"my-script\") => true
    (matches-pattern? \"anything\" \"*\")         => true"
  [filename pattern]
  (let [fs (FileSystems/getDefault)
        matcher (.getPathMatcher fs (str "glob:" pattern))
        path (.getPath fs filename (into-array String []))]
    (.matches matcher path)))

(defn matches-any-pattern?
  "Check if a filename matches any of the provided patterns.

  Parameters:
    filename - the filename to check
    patterns - collection of glob patterns

  Returns:
    true if the filename matches at least one pattern, false otherwise

  Examples:
    (matches-any-pattern? \"script.sh\" [\"*.sh\" \"*.cgi\"]) => true
    (matches-any-pattern? \"test.py\" [\"*.sh\" \"*.cgi\"])   => false"
  [filename patterns]
  (boolean (some #(matches-pattern? filename %) patterns)))

(defn filename-from-path
  "Extract just the filename from a file path.

  Examples:
    (filename-from-path \"scripts/my-script.sh\") => \"my-script.sh\"
    (filename-from-path \"my-script\")            => \"my-script\""
  [path]
  (last (clojure.string/split path #"[/\\\\]")))

(defn should-be-executable?
  "Determine if a file should be made executable based on configured patterns.

  Parameters:
    file-path - the path to the file
    patterns  - collection of allowed glob patterns

  Returns:
    true if the file matches any allowed pattern, false otherwise

  Security Note:
    Returns false if patterns is nil or empty (secure by default)"
  [file-path patterns]
  (if (and patterns (seq patterns))
    (let [filename (filename-from-path file-path)]
      (matches-any-pattern? filename patterns))
    false))
