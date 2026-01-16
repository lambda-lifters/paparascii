(ns lambda-lifters.paparascii.prism-js-highlighter
  (:require [clojure.java.io :as io]
            [hiccup2.core :as h])
  (:import (java.io File FileOutputStream IOException)
           (javax.script ScriptContext ScriptEngineManager ScriptException)
           (org.asciidoctor Asciidoctor)
           (org.asciidoctor.ast Document)
           (org.asciidoctor.extension LocationType)
           (org.asciidoctor.syntaxhighlighter Formatter HighlightResult Highlighter StylesheetWriter SyntaxHighlighterAdapter)))

#_((requiring-resolve 'hashp.install/install!))

;; Derived from: https://docs.asciidoctor.org/asciidoctorj/latest/syntax-highlighting/syntax-highlighter/
;; Notably: https://docs.asciidoctor.org/asciidoctorj/latest/syntax-highlighting/static-during-conversion/
(def ^:dynamic additional-header-css
  "dynamic variable containing any additional CSS (in fact, arbitrary code) to go into the header"
  nil)

(defonce highlighter-name "prismjs")

(defn write-stylesheet? [doc]
  (and (instance? Document doc) (.hasAttribute doc "linkcss") (.hasAttribute doc "copycss")))

(defn prism-css-string []
  (let [rsrc "prismjs/prism.css"]
    (try
      (str "<style>\n" (slurp (io/resource rsrc)) "\n</style>")
      (catch IOException e
        (throw (ex-info "exception raised reading css source resource" {:resource-path rsrc} e))))))

(defn get-engine- [_]
  (let [manager (ScriptEngineManager.)
        ;; Try GraalVM JS first, fallback to any JavaScript engine available
        script-engine (or (.getEngineByName manager "graal.js")
                         (.getEngineByName manager "JavaScript")
                         (throw (ex-info "No JavaScript engine available" {})))
        engine-factory (.getFactory script-engine)
        rsrc "prismjs/prism.js"]
    ;; Log which engine is being used for verification
    (println "🔧 JavaScript engine:"
             (.getEngineName engine-factory)
             "version:"
             (.getEngineVersion engine-factory))
    (try
      (with-open [rdr (io/reader (io/resource rsrc))]
        (.eval script-engine rdr))
      script-engine
      (catch ScriptException e
        (throw (ex-info "exception raised reading script resource" {:resource-path rsrc} e))))))

(def get-engine (memoize get-engine-))

(deftype PrismJsHighlighter []
  :load-ns true

  Formatter
  (format [_ node _ _]
    (set! additional-header-css (h/raw (prism-css-string)))
    (str "<pre class='highlight'><code>" (.getContent node) "</code></pre>"))

  Highlighter
  (highlight [this _ source lang _]
    (let [engine (get-engine this)
          ctx (.getContext engine)
          bindings (doto (.getBindings ^ScriptContext ctx ScriptContext/ENGINE_SCOPE)
                     (.put "text" source)
                     (.put "language" lang))
          ;; Check if language exists, fall back to plain text if not
          script "Prism.languages[language] ? Prism.highlight(text, Prism.languages[language], language) : text"]
      (try
        (HighlightResult. (.eval engine script bindings))
        (catch ScriptException e (throw (ex-info "exception raised running highlight script" {:script script :language lang} e))))))

  StylesheetWriter
  (isWriteStylesheet [_ document] (write-stylesheet? document))

  (writeStylesheet [_ _ to-dir]
    (with-open [in (io/reader (io/resource "prismjs/prism.css"))
                out (FileOutputStream. (File. to-dir "prism.css"))]
      (io/copy in out)))

  SyntaxHighlighterAdapter
  (hasDocInfo [_ l] (= l LocationType/HEADER))

  (getDocinfo [_ _ document _]
    (if (write-stylesheet? document)
      "<link href=\"prism.css\" rel=\"stylesheet\"/>"
      (prism-css-string))))

(defn register-highlighter-with-doctor [asciidoctor]
  (.register (.syntaxHighlighterRegistry ^Asciidoctor asciidoctor)
             PrismJsHighlighter
             (into-array String [highlighter-name])))
