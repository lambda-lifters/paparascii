(ns lambda-lifters.ascii-blog.log
  "logging functions, mostly aimed at the console for CLI use"
  (:require [clojure.string :as str]
            [clojure.stacktrace :as st]
            [lambda-lifters.lambda-liftoff.ansi :as ANSI])
  (:import (clojure.lang Delay)))

(def ^:dynamic prefix nil)
(def ^:dynamic handled-upstack? false)

(defn log [& strs] (println prefix (apply ANSI/FG-CYAN strs)))

(defn success [& strs]
  (doseq [l (str/split (apply str strs) #"\n")]
    (println prefix (ANSI/FG-BRIGHT-GREEN l))))

(defn error [& strs] (println prefix (apply ANSI/FG-BRIGHT-RED (ANSI/BOLD "ERROR: ") strs)))

(defn warn [& strs] (println prefix (apply ANSI/FG-BRIGHT-YELLOW (ANSI/BOLD "WARN: ") strs)))

(defmacro in-section [section-name & body]
  `(do
     (println prefix (ANSI/FG-CYAN "┌[" ~section-name))
     (try
       (binding [prefix (str prefix " " (ANSI/FG-CYAN "│"))] ~@body)
       (println prefix (str (ANSI/FG-CYAN "└[") (ANSI/FG-GREEN "✓") (ANSI/FG-CYAN "]")))
       (catch Exception e#
         (do (println prefix (str (ANSI/FG-RED "└") (ANSI/FG-BRIGHT-RED "[✗ : " (ex-message e#) "]")))
             (throw e#))))))

(defmacro in-application [application-name & body]
  `(do
     (println (ANSI/BOLD (ANSI/FG-BRIGHT-CYAN "┌[" ~application-name "]")))
     (try
       (binding [prefix (ANSI/FG-BRIGHT-CYAN "│")] ~@body)
       (println (ANSI/BOLD (ANSI/FG-BRIGHT-CYAN (.repeat "-" (+ 2 (count ~application-name))))))
       (catch Exception e#
         (do (println (ANSI/BOLD (ANSI/FG-BRIGHT-RED "└[✗ :" (with-out-str (st/print-stack-trace e#)))))
             (throw e#))))))

(defmacro log-action [action-name & body]
  `(do
     (when-not handled-upstack? (print prefix (ANSI/FG-YELLOW ~action-name)) (flush))
     (try
       ~@body
       (when-not handled-upstack? (println (str (ANSI/FG-BRIGHT-GREEN "✓"))))
       (catch Exception e#
         (do
           (if handled-upstack?
             (newline)
             (println (str (ANSI/FG-BRIGHT-RED "✗"))))
           (throw e#))))))

(defn monitor-parallel-actions!
  "Monitors the list of parallel actions until they have been completed.
  If monitoring is happening in an environment where Delays cannot be realized through an external force, then this
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
          (if-let [first-complete-job (some-> (some #(let [v (val %)]
                                                       (when (and force-delays? (delay? v)) (force v))
                                                       (when (realized? v) %))
                                                    jobs)
                                              key)]
            (let [jobs (dissoc jobs first-complete-job)]
              (println (str \return (ANSI/EL) prefix) (ANSI/FG-BRIGHT-GREEN first-complete-job))
              (when (seq jobs) (recur jobs (inc i))))
            (do (Thread/sleep 250) (recur jobs (inc i))))))
      pending-values)))