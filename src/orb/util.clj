(ns orb.util
  (require [clojure.instant :as inst]
           [orb.config      :as cfg]))

(defn- fix-org-date [s]
  (clojure.string/replace s
                          #"\s*([<>]|Sun|Mon|Tue|Wed|Thu|Fri|Sat)" ""))


(defn make-calendar [d]
  (clojure.instant/read-instant-calendar
   (fix-org-date d))) 

(defn make-date [d]
  (clojure.instant/read-instant-date 
   (fix-org-date d)))

(defn make-date-str [d]
  (let [d' (make-date d)]
    (.format 
     (java.text.SimpleDateFormat. (get-in @cfg/*siteconfig* [:dateformat] "EEE, dd MMM yyyy HH:mm:ss z"))
     d')))

