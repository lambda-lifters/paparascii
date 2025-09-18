(ns lambda-lifters.ascii-blog.log
  "logging functions, mostly aimed at the console for CLI use"
  (:require [lambda-lifters.ansi :as R]))

(defn section [& strs] (println (R/BOLD (apply R/FG-C+ strs))))

(defn log [& strs] (println (apply R/FG-C strs)))

(defn debug [& strs] (println (apply R/FG-Y strs)))

(defn success [& strs] (println (apply R/FG-G+ strs)))

(defn error [& strs] (println (apply R/FG-R+ (R/BOLD "ERROR: ") strs)))

(defn warn [& strs] (println (apply R/FG-Y+ (R/BOLD "WARN: ") strs)))