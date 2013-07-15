(ns orb.serve
  (:use ring.adapter.jetty
        ring.util.response
        ring.handler.dump
        ring.middleware.file)
  (:require [clojure.string :only (replace)]))

(def ^:dynamic *server* (agent nil))

(defn app [root]
  (-> handle-dump
      (wrap-file root {:index-files? true})))

(defn serve 
  [_ root port] 
  (run-jetty (app root) {:port port :join? false}))

(defn start-server 
  ([] (start-server "~/public_html/"))
  ([root] (start-server root 80))
  ([root port]
     (send-off *server* serve root port)))

(defn stop-server []
  (send-off *server* (fn [_] nil)))
