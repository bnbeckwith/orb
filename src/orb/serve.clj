(ns orb.serve
  (:use ring.adapter.jetty
        ring.util.response
        ring.handler.dump
        ring.middleware.file)
  (:require [clojure.string :only (replace)]))

(defn app [root]
  (wrap-file handle-dump root {:index-files? true}))

(defn serve 
  [root port] 
  (agent (run-jetty (app root) {:port port :join? false})))

(defn start-server [agt]
     (send-off agt (fn [j] (and (.start j) j))))

(defn stop-server [agt]
  (send-off agt (fn [j] (and (.stop j) j))))
