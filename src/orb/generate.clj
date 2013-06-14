(ns orb.generate
  (:import [org.apache.commons.lang3 StringUtils])
  (:require [orgmode.core :as org]
            [me.raynes.fs :as fs]
            [clj-rss.core :as rss]
            [orb.template.default :as tpl]))
            

(defn gen-locations
  [cfg]
  (let [fs (keys (get-in cfg [:conversion "org"]))
        n (apply merge
           (for [f fs]
             (let [e (get-in cfg [:conversion "org" f])
                   b (-> f
                         (StringUtils/removeStart (str (:root cfg) "/org/"))
                         (StringUtils/replace ".org" ".html"))]
                {f  (merge e
                     {:destination  (str java.io.File/separator b)})})))]
    ;; Handle N
    (assoc-in cfg [:conversion "org"] n)))
         

(defn gen-blog-files [cfg]
  (let [pfx (str (:root cfg) 
                 (java.io.File/separator)
                 (:output cfg))]
    (doseq [[f c] (get-in cfg [:conversion "org"])]
      (fs/mkdirs (str pfx (fs/parent (:destination c))))
        (with-open [w (clojure.java.io/writer (str pfx (:destination c)))]
          (.write w (tpl/make-page (deref (:org c)))))))
    cfg)
  

(defn gen-blog [cfg]
  (-> cfg
      gen-locations
      gen-blog-files))
      
(defn gen-static [cfg]
  "Copy over remaining static files"
  (doall (for [ft (keys (get-in cfg [:conversion])) 
               :when (not (= "org" ft))
               :when (not (re-matches #".*~$" ft)) ]
     (doseq [f (get-in cfg [:conversion ft])]
       (let [n (.toString f)
             dst (str (:root cfg) (java.io.File/separator) (:destination cfg)
                      (StringUtils/removeStart n (str (:root cfg) "/org")))]
         (if (fs/directory? n)
           (fs/mkdir dst)
           (fs/copy+ n dst))))))
  cfg)

(defn gen-indexes [cfg]
  cfg)

(defn gen-rss [cfg]
  (let [es (for [x (vals (get-in cfg [:conversion "org"]))
                 :let [e (deref (:org x))]
                 :when (when-let [cs (get-in e [:attribs "CATEGORY"])]
                         ((set (clojure.string/split
                                (clojure.string/lower-case cs)
                                #"\s+"))
                          "blog"))
                 :when (get-in e [:attribs "DATE"])
                 ]
             {:title (get-in e [:attribs "TITLE"])
              :link (get-in x [:destination])
              :description (get-in e [:attribs "DESCRIPTION"])
              :author (get-in e [:attribs "AUTHOR"] "none")
              :pubDate (.. (java.text.SimpleDateFormat. "yyyy-MM-dd")
                           (parse (subs (get-in e [:attribs "DATE"]) 0 10)))})
        feed
        (apply rss/channel-xml (into [{:title (:title cfg) :link (:site cfg) :description (:description cfg)}] es))]
    (with-open [w (clojure.java.io/writer (str (:root cfg) (java.io.File/separator) (:output cfg) (java.io.File/separator) "rss"))]
      (.write w feed)))
  cfg)


            
(defn generate [cfg]
  "Takes a cfg (after convert) and generates the contents"
  (-> cfg
      gen-blog
      gen-static
      gen-indexes
      gen-rss))
