#!/usr/bin/env bb

;; Example CGI script using Babashka
;; This will be copied to TARGET/public_html/cgi-bin/

(println "Content-Type: text/plain")
(println "")
(println "Hello from Babashka CGI!")
(println (str "Current time: " (java.util.Date.)))