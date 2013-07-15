(ns orb.generate
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

(defmulti gen-file
  "Generate a given file from the filename and conversion. Returns a
   map of {filename attributes}"
  (fn [_ cnv] (:type cnv)))

(defmethod gen-file :org [f cvn]
  (let [{:keys [name conversion]} cvn
        o (deref conversion)
        atrs (:attribs o)]
    (with-open [w (clojure.java.io/writer name)]
      (.write w (tpl/make-page o)))
    {name (merge atrs
                 (when-let [d (atrs :date)]
                   {:date (cvt/make-date (subs d 0 10))}))}))

(defmethod gen-file :default [f cvn]
  (let [{:keys [name conversion]} cvn]
    (if (nil? conversion)
      (fs/copy+ f name)
      (with-open [w (clojure.java.io/writer name)]
        (.write cvn)))
    {name nil}))

; TODO -- handle nil case from convert
(defn convert-and-generate 
  "Run convert on the provided file and then generate on those results."
  [cfg f]
  (let [cnv (cvt/convert f)
        ext (:ext cnv)
        dst (of/change-ext (of/to-destination f cfg) ext)]
;    (println "Name: " dst " -- File: " f)
    (gen-file f (merge cnv {:name dst}))))

(defn gen-files 
  "Create the target locations and handle the file conversion and generation.
  Merge in a map of dest-filename to attributes for indexing/rss to
  cfg as :attribs
"
  [cfg]
  (gen-locations cfg)
  (merge cfg
         {:attribs
          (apply (partial merge {})
                 (map (partial convert-and-generate cfg)
                      (filter fs/file? 
                              (:source-files cfg))))}))

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

(defn gen-blog
  "Generate The main index and associated rss file"
  [cfg]
  (let [sitetitle (:title cfg)
        siteurl   (:site cfg)
        sitedesc  (:description cfg)
        getd     (fn [s] (if-let [m (second s)]
                           (:date m)))
        pairs    (reverse (sort-by getd (filter getd (seq (:attribs cfg)))))
        es (for [[f a] pairs
                 :when (not (nil? a))
                 :when (when-let [cs (get-in a [:category])]
                         ((set (clojure.string/split
                                (clojure.string/lower-case cs)
                                #"\s+"))
                          "blog"))
                 :when (get-in a [:date])]
             {:title (:title a)
              :link  (of/make-url f cfg "")
              :description (:description a)
              :author (get-in a [:author] "None")
              :pubDate (:date a)})
        feed (apply rss/channel-xml
                    (into [{:title sitetitle
                            :link  siteurl
                            :description sitedesc}]
                          (take (get-in cfg [:rss-entries-limit] 10) es)))]
    (with-open [w (clojure.java.io/writer (str (:output cfg) (java.io.File/separator) "rss"))]
      (.write w feed))
    (with-open [w (clojure.java.io/writer (str (:output cfg) (java.io.File/separator) "index.html"))]
      (.write w (tpl/make-index {:title "Blog"} es))))
  cfg)


(defn generate [cfg]
  "Takes a cfg and generates the contents performing conversions"
  (-> cfg
      gen-files
      gen-indexes
      gen-blog
      ))
