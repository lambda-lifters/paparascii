(ns lambda-lifters.paparascii.log
  "logging functions, mostly aimed at the console for CLI use"
  (:require [clojure.stacktrace :as st]
            [clojure.string :as str]
            [lambda-lifters.lambda-liftoff.ansi :as ANSI]))

(def ^:dynamic prefix nil)
(def ^:dynamic handled-upstack? false)

(defn log [& strs] (println prefix (apply ANSI/FG-CYAN strs)))

(defn success [& strs]
  (doseq [l (str/split (apply str strs) #"\n")]
    (println prefix (ANSI/FG-BRIGHT-GREEN l))))

(defn error [& strs]
  (println prefix (apply ANSI/FG-BRIGHT-RED (ANSI/BOLD "ERROR: ") strs)))

(defn warn [& strs]
  (println prefix (apply ANSI/FG-BRIGHT-YELLOW (ANSI/BOLD "WARN: ") strs)))

(defmacro in-section [section-name & body]
  `(do
     (println prefix (ANSI/FG-CYAN "┌[" ~section-name))
     (try
       (let [rv# (binding [prefix (str prefix " " (ANSI/FG-CYAN "│"))] ~@body)]
         (println prefix (str (ANSI/FG-CYAN "└[") (ANSI/FG-GREEN "✓") (ANSI/FG-CYAN "]")))
         rv#)
       (catch Exception e#
         (do (println prefix (str (ANSI/FG-RED "└") (ANSI/FG-BRIGHT-RED "[✗ : " (ex-message e#) "]")))
             (throw e#))))))

(comment
  (macroexpand-1
    '(log-action "foo" 1 2 3))
  (log-action "foo" 1 2 3)
  (in-section "bar"
              (log-action "foo" 1 2 3))
  (in-application "baz"
                  (in-section "bar"
                              (log-action "foo" 1 2 3)))
  )

(defmacro in-application [application-name & body]
  `(do
     (println (ANSI/BOLD (ANSI/FG-BRIGHT-CYAN "┌[" ~application-name "]")))
     (try
       (let [rv# (binding [prefix (ANSI/FG-BRIGHT-CYAN "│")] ~@body)]
         (println (ANSI/BOLD (ANSI/FG-BRIGHT-CYAN (.repeat "-" (+ 2 (count ~application-name))))))
         rv#)
       (catch Exception e#
         (do (println (ANSI/BOLD (ANSI/FG-BRIGHT-RED "└[✗ :" (with-out-str (st/print-stack-trace e#)))))
             (throw e#))))))

(defmacro log-action [action-name & body]
  `(do
     (when-not handled-upstack? (print prefix (ANSI/FG-YELLOW ~action-name)) (flush))
     (try
       (let [rv# (do ~@body)]
         (when-not handled-upstack? (println (str (ANSI/FG-BRIGHT-GREEN "✓"))))
         rv#)
       (catch Exception e#
         (do
           (if handled-upstack?
             (newline)
             (println (str (ANSI/FG-BRIGHT-RED "✗"))))
           (throw e#))))))

(defn some-completed-job [jobs force-delays?]
  (some #(let [v (val %)]
           (when (and force-delays? (delay? v)) (force v))
           (when (realized? v) %))
        jobs))

(defn monitor-parallel-actions!
  "Monitors the list of parallel actions until they have been completed.
  If monitoring is happening in an environment where Delays cannot be realised through an external force, then this
  loop will not terminate. :force-delays? (boolean) is provided which, when true (the default) forces delays within the
  loop."
  [action-name *pending-value-map & {:keys [force-delays?] :or {force-delays? true}}]
  (println prefix (ANSI/FG-BRIGHT-YELLOW "┌[Multiple actions: " action-name "]"))
  (binding [handled-upstack? true
            prefix (str prefix " " (ANSI/FG-BRIGHT-YELLOW "│"))]
    (let [pending-values (deref *pending-value-map)
          jobs (into {} pending-values)]
      (loop [jobs jobs i 0]
        (when (seq jobs)
          (print (str \return (ANSI/EL) prefix) (quot i 4) (ANSI/FG-YELLOW (str/join ", " (map key jobs))))
          (flush)
          (if-let [first-complete-job (some-> (some-completed-job jobs force-delays?) key)]
            (let [jobs (dissoc jobs first-complete-job)]
              (println (str \return (ANSI/EL) prefix) (ANSI/FG-BRIGHT-GREEN first-complete-job))
              (when (seq jobs) (recur jobs (inc i))))
            (do (Thread/sleep 250) (recur jobs (inc i))))))
      pending-values)))
