(ns lambda-lifters.paparascii.prism-js-highlighter-test
  (:require [clojure.test :refer :all]
            [lambda-lifters.paparascii.prism-js-highlighter :as prism])
  (:import (javax.script ScriptEngineManager)
           (lambda_lifters.paparascii.prism_js_highlighter PrismJsHighlighter)))

(deftest test-graalvm-engine-available
  (testing "GraalVM JS engine is available"
    (let [manager (ScriptEngineManager.)
          engine (.getEngineByName manager "graal.js")]
      (is (some? engine) "GraalVM JS engine should be available")
      (when engine
        (let [factory (.getFactory engine)]
          (is (= "Graal.js" (.getEngineName factory))
              "Engine name should be 'Graal.js'")
          (println "✅ Engine name:" (.getEngineName factory))
          (println "✅ Engine version:" (.getEngineVersion factory)))))))

(deftest test-engine-basic-evaluation
  (testing "GraalVM JS can evaluate basic JavaScript"
    (let [manager (ScriptEngineManager.)
          engine (.getEngineByName manager "graal.js")]
      (is (some? engine) "Engine should be available")
      (when engine
        (let [result (.eval engine "2 + 2")]
          (is (= 4 result) "Basic arithmetic should work"))
        (let [result (.eval engine "var x = 'hello'; x.toUpperCase()")]
          (is (= "HELLO" result) "String operations should work"))))))

(deftest test-prism-engine-initialization
  (testing "Prism highlighter engine initializes with GraalVM"
    (let [highlighter (PrismJsHighlighter.)
          engine (prism/get-engine highlighter)]
      (is (some? engine) "Prism engine should initialize")
      (let [factory (.getFactory engine)
            engine-name (.getEngineName factory)]
        (is (= "Graal.js" engine-name)
            (str "Should be using GraalVM JS, but got: " engine-name))
        (println "✅ Prism using:" engine-name)))))

(deftest test-prism-highlight-functionality
  (testing "Prism can highlight code using GraalVM JS"
    (let [highlighter (PrismJsHighlighter.)
          ;; Test with a simple JavaScript snippet
          source "(defn hello [] (println \"world\"))"
          lang "clojure"]
      (try
        (let [result (.highlight highlighter nil source lang nil)]
          (is (some? result) "Highlight should return a result")
          (println "✅ Successfully highlighted" lang "code"))
        (catch Exception e
          (is false (str "Highlighting failed: " (.getMessage e))))))))

(deftest test-engine-caching
  (testing "Engine is cached per highlighter instance"
    (let [highlighter (PrismJsHighlighter.)
          engine1 (prism/get-engine highlighter)
          engine2 (prism/get-engine highlighter)]
      (is (identical? engine1 engine2)
          "Same highlighter should reuse its engine instance")
      (println "✅ Engine caching per instance working correctly"))))

