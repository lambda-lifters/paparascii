(ns kaocha.reporter.with-skip-count
  "Custom Kaocha reporter that tracks skip counts.
   Designed to be chained with kaocha.report/documentation."
  (:require [kaocha.output :as output]
            [clojure.pprint :as pprint]))

(def ^:private *skip-count
  "Atom tracking the number of skipped tests in the current run"
  (atom 0))

(defn- reset-skip-count! []
  (reset! *skip-count 0))

(defn report
  "Custom reporter function that tracks skip counts.
   Designed to be used alongside kaocha.report/documentation in a reporter chain.
   Configuration: :reporter [kaocha.report/documentation kaocha.reporter.with-skip-count/report]

   Note: Due to Kaocha's architecture, tests with ^:kaocha/skip metadata are filtered
   out before the test plan is created, so they won't appear in skip counts."
  [m]
  ;; Handle skip counting (though ^:kaocha/skip tests won't fire these events)
  (when (= :kaocha/skip (:type m))
    (swap! *skip-count inc))

  ;; Reset counter at start of test run
  (when (= :begin-test-run (:type m))
    (reset-skip-count!))

  ;; Add skip count after summary
  (when (= :summary (:type m))
    (let [skip-count (or (:pending m) 0)]
      (when (pos? skip-count)
        (println (output/colored :yellow
                                 (pprint/cl-format nil "~d test~:p skipped." skip-count)))))))
