(ns lambda-lifters.paparascii.serve
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [lambda-lifters.paparascii.path-security :as path-sec]
            [lambda-lifters.paparascii.site :as site])
  (:import (com.sun.net.httpserver HttpHandler HttpServer)
           (java.net InetSocketAddress)
           (java.nio.file Files)))

(def content-types
  "Map of file extensions to MIME content types"
  {"html"  "text/html"
   "css"   "text/css"
   "js"    "application/javascript"
   "json"  "application/json"
   "pdf"   "application/pdf"
   "png"   "image/png"
   "jpg"   "image/jpeg"
   "jpeg"  "image/jpeg"
   "gif"   "image/gif"
   "svg"   "image/svg+xml"
   "ico"   "image/x-icon"
   "woff"  "font/woff"
   "woff2" "font/woff2"
   "ttf"   "font/ttf"
   "eot"   "application/vnd.ms-fontobject"})

(defn file-extension
  "Extract file extension from path"
  [path]
  (some->> (str/last-index-of path ".")
           inc
           (subs path)))

(defn content-type
  "Get content-type for file extension"
  [path]
  (get content-types (file-extension path) "application/octet-stream"))

(defn resolve-index-file [requested-file relative-path]
  (if (.isDirectory requested-file)
    (str relative-path "index.html")
    relative-path))

(defn copy-file-to-exchange! [exchange file path]
  (let [content (Files/readAllBytes (.toPath file))
        headers (.getResponseHeaders exchange)]
    ;; Set content type based on file extension
    (.add headers "Content-Type" (content-type path))
    (.sendResponseHeaders exchange 200 (alength content))
    (with-open [os (.getResponseBody exchange)]
      (.write os content))))

(defn respond-error-to-exchange! [exchange status-code]
  (.sendResponseHeaders exchange status-code 0)             ; File not found
  (.close (.getResponseBody exchange)))

(defn serve-file
  "Serve a static file with path traversal protection"
  [exchange ^String root-dir ^String path]
  (try
    ;; Strip leading slash to make path relative
    (let [path (if (= "/" path) "/index.html" path)
          relative-path (if (str/starts-with? path "/") (subs path 1) path)
          requested-file (io/file root-dir relative-path)
          relative-path (resolve-index-file requested-file relative-path)
          file (io/file root-dir relative-path)]
      (path-sec/validate-path! root-dir file)               ; check after potential index.html append
      (if (.exists file)
        (copy-file-to-exchange! exchange file path)
        (respond-error-to-exchange! exchange 404)))
    (catch SecurityException _
      (log/warn "Security: Path traversal blocked:" path)
      (respond-error-to-exchange! exchange 403))))

(defn create-handler
  "Create HTTP handler for static file serving"
  [root-dir]
  (reify HttpHandler
    (handle [_ exchange]
      (try
        (->> exchange
             .getRequestURI
             .getPath
             (serve-file exchange root-dir))
        (catch Exception e
          (log/error e "Error handling request")
          (respond-error-to-exchange! exchange 500))))))

(defn start-server
  "Start HTTP server using Java's built-in HttpServer"
  [{:keys [port dir] :or {port 8000 dir (site/public-html-path)}}]
  (log/info "Starting HTTP server...")
  (log/info "Serving from:" dir)
  (log/info "URL:" (str "http://localhost:" port))
  (log/info "Press Ctrl+C to stop")
  (let [server (HttpServer/create (InetSocketAddress. port) 0)]
    (.createContext server "/" (create-handler dir))
    (.setExecutor server nil)
    (.start server)
    (log/info "Server started successfully!")
    ;; Keep the server running
    @(promise)))

(defn -main
  "Main entry point for standalone execution"
  [& _]
  (start-server {:port 8000 :dir (site/public-html-path)}))
