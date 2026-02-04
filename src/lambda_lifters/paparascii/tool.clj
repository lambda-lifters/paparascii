(ns lambda-lifters.paparascii.tool
  (:require [lambda-lifters.paparascii.build :as build]
            [lambda-lifters.paparascii.clean :as clean]
            [lambda-lifters.paparascii.init :as init]
            [lambda-lifters.paparascii.list-posts :as list-posts]
            [lambda-lifters.paparascii.new-post :as new-post]
            [lambda-lifters.paparascii.serve :as serve]
            [clojure.main :as main]))

;; Tool entry points
(defn clean
  "Clean the TARGET directory"
  [& {:as args}]
  (clean/clean-target! args))

(defn build
  "Build the static site"
  [& {:as args}]
  (build/build! args))

(defn serve
  "Start development server"
  [& {:as args}]
  (serve/start-server args))

(defn new-post
  "Create a new blog post"
  [& {:as args}]
    (new-post/create-post args))

(defn list-posts
  "List all blog posts"
  [& {:as args}]
  (list-posts/list-all-posts args))

(defn init
  "Initialise a new blog"
  [& {:as args}]
  (init/init! args))

(defn paparascii-reload
  "Reload all paparascii namespaces (for local development)"
  []
  (require 'lambda-lifters.paparascii.tool :reload-all)
  (println "Reloaded paparascii namespaces"))

(defn repl
  "Start an interactive REPL with all tool functions available"
  [& {:as _args}]
  (println "paparascii REPL")
  (println "Available: (build) (serve) (clean) (init) (new-post) (list-posts)")
  (println "           (paparascii-reload) - reload source for development")
  (println "")
  (in-ns 'user)
  (refer 'lambda-lifters.paparascii.tool)
  (main/repl))
