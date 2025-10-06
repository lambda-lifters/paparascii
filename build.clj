(ns build
  "Build configuration for paparascii as an installable tool"
  (:require [clojure.tools.build.api :as b]))

(def lib 'lambda-lifters/paparascii)
(def version "0.1.0")
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-file (format "target/%s-%s.jar" (name lib) version))
(def uber-file (format "target/%s-%s-standalone.jar" (name lib) version))

(defn clean [_]
  (b/delete {:path "target"}))

(defn jar [_]
  (b/write-pom {:class-dir class-dir
                :lib lib
                :version version
                :basis basis
                :src-dirs ["src"]})
  (b/copy-dir {:src-dirs ["src" "resources"]
               :target-dir class-dir})
  #_(b/compile-clj {:basis basis
                  :ns-compile '[lambda-lifters/paparascii.tool lambda-lifters/paparascii.prism-js-highlighter]
                  :class-dir class-dir
                  })
      (b/jar {:class-dir class-dir
              :jar-file  jar-file}))

;; Tool configurations - these make it installable
(def tool-edn
  {:lib lib
   :coord {:local/root "."} ; For local testing
   ;; For git installation:
   ;; :coord {:git/url "https://github.com/lambda-lifters/paparascii.git"
   ;;         :git/sha "LATEST_SHA"}
   })
