(ns lambda-lifters.ascii-blog.tool
  (:require [lambda-lifters.ascii-blog.build :as build]
            [lambda-lifters.ascii-blog.clean :as clean]
            [lambda-lifters.ascii-blog.init :as init]
            [lambda-lifters.ascii-blog.list-posts :as list-posts]
            [lambda-lifters.ascii-blog.new-post :as new-post]
            [lambda-lifters.ascii-blog.serve :as serve]))

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
