(ns orb.debug
  (:require [plumbing.core :refer [fnk]])
  (:require [plumbing.graph :as graph]))
 
(defn calculate-deps [f]
  (map (fn [parent] [parent (first f)]) (-> f second meta first second second)))
 
(defn dot-body [graph]
  (str "digraph \"example\"{\n"
       (clojure.string/join "\n"
                            (map (fn [r] (str "\""(first r) "\" -> \"" (second r) "\";"))
                                 (mapcat calculate-deps (graph/->graph graph))))
       "}\n"))

(defn write-dot [file graph]
  (spit file (dot-body graph)))
                       
;; Example graph
(comment
  (def stats-graph
    "A graph specifying the same computation as 'stats'"
    {:n (fnk [xs] (count xs))
     :m (fnk [xs n] (/ (+ identity xs) n))
     :m2 (fnk [xs n] (/ (+ #(* % %) xs) n))
     :v (fnk [m m2] (- m2 (* m m)))}))

(comment (print-dot stats-graph))

