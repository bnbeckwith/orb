(ns orb.generate
  (:use [plumbing.core])
  (:require [orgmode.core :as org]
            [me.raynes.fs :as fs]
            [clj-rss.core :as rss]
            [orb.convert  :as cvt]
            [orb.file     :as of]
            [clojure.string :as cs]
            [clojure.pprint]))

(defn gen-locations
  "Generate all of the matching output directories"
  [cfg]
  (map #(fs/mkdirs (of/to-destination %1 cfg))
       (filter fs/directory? (:source-files cfg))))

(defmulti gen-file!
   "Generate a given file from the filename and conversio n.
    Conversion is expected to have a key :name indicating destination name"
   (fn [_ tplfs cnv] (:type cnv :default)))

(defmethod gen-file! :org [f tplfs cvn]
  (let [{:keys [name conversion]} cvn
        h (org/convert conversion)
        a (get-in conversion [:attribs])
        t (if (get-in a [:tags :blog])
            (:post tplfs)
            (:page tplfs))
        p (t (merge cvn {:attribs a :html h :templates tplfs}))]
    (with-open [w (clojure.java.io/writer name)]
      (.write w (clojure.string/join p)))))

(defmethod gen-file! :dir [f tplfs cvn]
  (let [{:keys [name]} cvn]
    (when name
      (fs/mkdirs name))))

(defmethod gen-file! :default [f tplfs cvn]
   (let [{:keys [name conversion]} cvn]
     (if (nil? conversion)
       (fs/copy+ f name)
       (with-open [w (clojure.java.io/writer name)]
         (.write w cvn)))))

(defn rssfn
  "Return the rss generating function. Accepts filename as argument"
  ([] (rssfn "rss"))
  ([f] (fnk [blog-entries to sitemeta]
            (let [sitetitle (:title sitemeta)
                  siteurl   (:baseurl sitemeta)
                  sitedesc  (:description sitemeta)
                  es (for [e blog-entries
                           :let [a (get-in e [:conversion :attribs])]]
                       {:title (:title a)
                        :link  (:url e)
                        :description (:description a)
                        :author (get-in a [:author] "Unknown")
                        :pubDate (clojure.instant/read-instant-date (:date a))})
                  feed (apply rss/channel-xml
                     (into [{:title sitetitle
                             :link  siteurl
                             :description sitedesc}]
                           (take (get-in sitemeta [:rss-entries-limit] 10) es)))]
     (with-open [w (clojure.java.io/writer (str to (java.io.File/separator) f))]
       (.write w feed))))))

(defn newer-older-fnames 
  ([f] (newer-older-fnames 
         f
         (Integer/parseInt (or (re-find #"\d+(?=.)" f) "0"))))
  ([f n]
    (let [nx    (dec n)
          newer (cond (zero? nx) "index.html"
                      (pos?  nx) (str "index" nx ".html")
                      :else nil)
          older (str "index" (inc n) ".html")]
      [older newer])))
  

(defn make-index!
  ([es to tfns sm] (make-index! es to tfns sm "index.html"))
  ([es to tfns sm f]
     (let [ppp (get-in sm [:index-posts-per-page] 10)
           ps  (take ppp es)
           ops (drop ppp es)
           ns  (newer-older-fnames f)
           ctx {:newer (second ns)
                :older (when (not (empty? ops)) (first ns))
                :attribs {:title "Index"}
                :templates tfns }
           idx ((:index tfns) ctx ps)
           ]
       (with-open [w (clojure.java.io/writer (str to java.io.File/separator f))]
         (.write w (clojure.string/join idx))
       (when (seq ops)
         (make-index! ops to tfns sm (first ns)))))))

(defnk blogindexfn [blog-entries to templatefns sitemeta]
  (make-index! blog-entries to templatefns sitemeta "blog/index.html"))

(defnk indexfn [blog-entries to templatefns sitemeta]
  (make-index! blog-entries to templatefns sitemeta))
  

(defn make-summary! [gs ctx fname]
  (let [f (fs/file fname)
        tpl (get-in ctx [:templates :summary])
        d (if (fs/directory? f)
            f
            (fs/parent f))]
    (fs/mkdirs d)
    (with-open [w (clojure.java.io/writer fname)]
      (.write w (clojure.string/join (tpl ctx gs))))))

(defn makeset [path element]
  (when-let [xs (get-in element path)]
    (->> xs
         (cs/lower-case)
         (cs/trim)
         (#(cs/split % #"\s+"))
         (map keyword)
         (set))))

(defn addtags [es]
  (for [e es
        :let [ts (makeset [:conversion :attribs :filetags] e)]]
    (merge e {:tags ts})))
    
(defn group-by-tags [es]
  (let [allts (apply clojure.set/union (map :tags es))]
    (apply merge (for [t (sort allts)
                 :let [ms (filter (comp t :tags) es)]]
             {t ms}))))

(defnk maketags [elements to templatefns sitemeta]
  (let [es (addtags elements)
        groups (group-by-tags es)]
    (make-summary! groups {:attribs {:title "Tags"} :templates templatefns}
                   (str to java.io.File/separator "tags/index.html"))))

(defnk makecategories [elements to templatefns sitemeta]
  (let [es (for [e elements
                 :let [cs (makeset [:conversion :attribs :category] elements)]]
             (merge e {:categories cs}))
        allcats (apply clojure.set/union (map :categories es))]
    (doseq [c allcats
            l (filter (comp c :categories es))
            n (name c)
            d (str to "/categories/" n)]
      (fs/mkdirs d)
      (make-index! l to templatefns sitemeta (str "categories/" n "/index.html")))))


(defnk makearchive [blog-entries to templatefns sitemeta]
  (let [es (for [e blog-entries
                 :let [date (clojure.instant/read-instant-calendar
                             (get-in e [:conversion :attribs :date]))]]
             (merge e 
                    {:date  date
                     :year  (.get date java.util.Calendar/YEAR)
                     :month (.get date java.util.Calendar/MONTH)
                     :day   (.get date java.util.Calendar/DAY_OF_MONTH)}))
        gs (group-by :year es)
        sorted-gs (apply (partial sorted-map-by >) (interleave (keys gs) (vals gs)))]
    (make-summary! sorted-gs
                   {:attribs {:title "Archive"} :templates templatefns} 
                   (str to java.io.File/separator "archive/index.html"))))
                   
