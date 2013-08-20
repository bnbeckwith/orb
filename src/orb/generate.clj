(ns orb.generate
  (:use [plumbing.core])
  (:require [orgmode.core :as org]
            [me.raynes.fs :as fs]
            [clj-rss.core :as rss]
            [orb.convert  :as cvt]
            [orb.file     :as of]
            [orb.template.default :as tpl]))

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
      (.write w (apply str p)))))

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

; TODO -- handle nil case from convert
;; (defn convert-and-generate 
;;   "Run convert on the provided file and then generate on those results."
;;   [cfg f]
;;   (let [cnv (cvt/convert f)
;;         ext (:ext cnv)
;;         dst (of/change-ext (of/to-destination f cfg) ext)]
;; ;    (println "Name: " dst " -- File: " f)
;;     (gen-file f (merge cnv {:name dst}))))

;; (defn gen-files 
;;   "Create the target locations and handle the file conversion and generation.
;;   Merge in a map of dest-filename to attributes for indexing/rss to
;;   cfg as :attribs
;; "
;;   [cfg]
;;   (gen-locations cfg)
;;   (merge cfg
;;          {:attribs
;;           (apply (partial merge {})
;;                  (map (partial convert-and-generate cfg)
;;                       (filter fs/file? 
;;                               (:source-files cfg))))}))

(defn gen-tags
  [cfg]
  cfg)

(defn gen-archive
  [cfg]
  cfg)

(defn gen-indexes 
  "Generate indexes for the given files"
  [cfg]
  (-> cfg
      gen-tags
      gen-archive)
  cfg)

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
          newer (cond (= nx 0) "index.html"
                      (> nx 0) (str "index" nx ".html")
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
           idx ((:index tfns) {:newer (second ns) :older (when (not (empty? ops)) (first ns))} ps)
           ]
       (with-open [w (clojure.java.io/writer (str to java.io.File/separator f))]
         (.write w (apply str idx))
       (when (not (empty? ops))
         (make-index! ops to tfns sm (first ns)))))))

(defnk indexfn [blog-entries to templatefns sitemeta]
  (make-index! blog-entries to templatefns sitemeta))
  

     ;; (with-open [w (clojure.java.io/writer (str to (java.io.File/separator) "index.html"))]
     ;;   (.write w (tpl/make-index {:title "Blog"} es)))))))

(defn generate [cfg]
  "Takes a cfg and generates the contents performing conversions"
  (-> cfg
;;      gen-files
      gen-indexes
;;      gen-blog
      ))
