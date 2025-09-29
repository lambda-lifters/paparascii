(ns lambda-lifters.ascii-blog.serve
  (:import [com.sun.net.httpserver HttpServer HttpHandler]
           [java.net InetSocketAddress]
           [java.io File]
           [java.nio.file Files]))

(defn get-site-dir
  "Get the site directory from environment or current directory"
  []
  (or (System/getenv "ASCII_SITE_DIR") "."))

(defn serve-file
  "Serve a static file"
  [exchange ^String root-dir ^String path]
  (let [file (File. root-dir path)
        path (if (and (.isDirectory file) (.endsWith path "/"))
               (str path "index.html")
               path)
        file (File. root-dir path)]
    (if (.exists file)
      (let [content (Files/readAllBytes (.toPath file))
            headers (.getResponseHeaders exchange)]
        ;; Set content type based on file extension
        (cond
          (.endsWith path ".html") (.add headers "Content-Type" "text/html")
          (.endsWith path ".css") (.add headers "Content-Type" "text/css")
          (.endsWith path ".js") (.add headers "Content-Type" "application/javascript")
          (.endsWith path ".json") (.add headers "Content-Type" "application/json")
          :else (.add headers "Content-Type" "application/octet-stream"))
        (.sendResponseHeaders exchange 200 (alength content))
        (with-open [os (.getResponseBody exchange)]
          (.write os content)))
      ;; File not found
      (do
        (.sendResponseHeaders exchange 404 0)
        (.close (.getResponseBody exchange))))))

(defn create-handler
  "Create HTTP handler for static file serving"
  [root-dir]
  (reify HttpHandler
    (handle [_ exchange]
      (let [path (.getPath (.getRequestURI exchange))
            path (if (= "/" path) "/index.html" path)]
        (serve-file exchange root-dir path)))))

(defn start-server
  "Start HTTP server using Java's built-in HttpServer"
  [{:keys [port dir] :or {port 8000 dir (str (get-site-dir) "/TARGET/public_html")}}]
  (println "\n🌐 Starting HTTP server...")
  (println (str "📁 Serving from: " dir))
  (println (str "🔗 URL: http://localhost:" port))
  (println "⏹️  Press Ctrl+C to stop\n")

  (let [server (HttpServer/create (InetSocketAddress. port) 0)]
    (.createContext server "/" (create-handler dir))
    (.setExecutor server nil)
    (.start server)
    (println "Server started successfully!")

    ;; Keep the server running
    @(promise)))

(defn -main
  "Main entry point for standalone execution"
  [& _]
  (start-server {:port 8000 :dir (str (get-site-dir) "/TARGET/public_html")}))