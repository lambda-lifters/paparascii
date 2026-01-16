(ns lambda-lifters.paparascii.serve
  (:require [lambda-lifters.paparascii.site :as site]
            [lambda-lifters.paparascii.path-security :as path-sec]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log])
  (:import (com.sun.net.httpserver HttpHandler HttpServer)
           (java.io File)
           (java.net InetSocketAddress)
           (java.nio.file Files)))

(def content-types
  "Map of file extensions to MIME content types"
  {"html" "text/html"
   "css"  "text/css"
   "js"   "application/javascript"
   "json" "application/json"
   "pdf"  "application/pdf"
   "png"  "image/png"
   "jpg"  "image/jpeg"
   "jpeg" "image/jpeg"
   "gif"  "image/gif"
   "svg"  "image/svg+xml"
   "ico"  "image/x-icon"
   "woff" "font/woff"
   "woff2" "font/woff2"
   "ttf"  "font/ttf"
   "eot"  "application/vnd.ms-fontobject"})

(defn file-extension
  "Extract file extension from path"
  [path]
  (some-> (str/last-index-of path ".")
          inc
          (->> (subs path))))

(defn content-type
  "Get content-type for file extension"
  [path]
  (get content-types (file-extension path) "application/octet-stream"))

(defn serve-file
  "Serve a static file with path traversal protection"
  [exchange ^String root-dir ^String path]
  (try
    ;; Strip leading slash to make path relative
    (let [relative-path (if (str/starts-with? path "/") (subs path 1) path)
          requested-file (io/file root-dir relative-path)
          _ (path-sec/validate-path! root-dir requested-file)

          ;; Handle directory index files
          relative-path (if (and (.isDirectory requested-file) (str/ends-with? relative-path "/"))
                          (str relative-path "index.html")
                          relative-path)
          file (io/file root-dir relative-path)]

      ;; Second validation: re-check after potential index.html append
      (path-sec/validate-path! root-dir file)

      (if (.exists file)
        (let [content (Files/readAllBytes (.toPath file))
              headers (.getResponseHeaders exchange)]
          ;; Set content type based on file extension
          (.add headers "Content-Type" (content-type path))
          (.sendResponseHeaders exchange 200 (alength content))
          (with-open [os (.getResponseBody exchange)]
            (.write os content)))
        ;; File not found
        (do
          (.sendResponseHeaders exchange 404 0)
          (.close (.getResponseBody exchange)))))

    (catch SecurityException e
      ;; Path traversal attempt detected
      (log/warn "Security: Path traversal blocked:" path)
      (.sendResponseHeaders exchange 403 0)
      (.close (.getResponseBody exchange)))))

(defn create-handler
  "Create HTTP handler for static file serving"
  [root-dir]
  (reify HttpHandler
    (handle [_ exchange]
      (try
        (let [path (.getPath (.getRequestURI exchange))
              path (if (= "/" path) "/index.html" path)]
          (serve-file exchange root-dir path))
        (catch Exception e
          (log/error e "Error handling request")
          (.sendResponseHeaders exchange 500 0)
          (.close (.getResponseBody exchange)))))))

(defn start-server
  "Start HTTP server using Java's built-in HttpServer"
  [{:keys [port dir] :or {port 8000 dir (site/public-html-path)}}]
  (log/info "Starting HTTP server...")
  (log/info (str "Serving from: " dir))
  (log/info (str "URL: http://localhost:" port))
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
