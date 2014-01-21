(ns orb.org
  (:require [clojure.zip :as zip]
            [orgmode.html :as oh]))


(defmulti fix-org-node oh/elmtype)

(defmethod fix-org-node :link [n]
  (merge n
         (when-let [ms (re-matches #"file:(.*)\.org" (:uri n))]
           {:uri (str (second ms) ".html")})))

(defmethod fix-org-node :default [n]
  n)

(defn fix-org-nodes [o]
  (loop [z (zip/xml-zip o)]
    (if (zip/end? z)
      (zip/root z)
      (recur (zip/next
              (zip/edit z fix-org-node))))))
