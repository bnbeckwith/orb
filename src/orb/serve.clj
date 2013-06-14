(ns orb.serve
  (:use ring.adapter.jetty
        ring.util.response
        ring.middleware.file)
  (:require [clojure.string :only (replace)]))

(defn list-files [request]
  (let [{:keys [uri root site-name site-port scheme]} request
        p (replace (str root uri) "//" "/")]
    (if (.exists (java.io.File. (str p "index.html")))
      (redirect (str uri "index.html"))
      (response
       (apply str
              (for [f (.listFiles (java.io.File. p))
                    :let [fm (replace (str f) p "")
                          fn (if (.isDirectory f) (str fm "/") fm)]]
                (format "<a href=\"%s\">%s</a><br>" fn fn)))))))

(defn make-handler [root]
  (fn [request]
    (if (re-find #"/$" (:uri request))
      (list-files (merge request {:root root}))
      (response "<H1>NOT FOUND</H1>"))))

(defn app [root]
  (wrap-file (make-handler root) root))

(defn serve [root port]
  (run-jetty (app root) {:port port}))