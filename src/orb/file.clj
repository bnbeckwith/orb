(ns orb.file
  (:import [java.nio.file StandardWatchEventKinds FileSystem WatchKey])
  (:require [clojure.java.io :as io]
            [clj-time.core :as ctc]
            [clj-time.format :as ctf]
            [me.raynes.fs :as fs]
            [cemerick.pomegranate :as pom])
  (:gen-class))


(defn make-path [r c]
  "Join r and c with the default filesystem separator"
  (fs/expand-home 
   (clojure.string/join 
    (java.io.File/separator)
    [r c])))

(defn add-sources [cfg]
  "Look in the path specified by :source in cfg and add files to :source-files"
  (merge cfg
         {:source-files
          (file-seq 
           (clojure.java.io/file 
            (make-path (:root cfg)
                       (:source cfg))))}))

(defn add-classpath-and-load! [path]
  (pom/add-classpath path)
  (map #(load-file (.getAbsolutePath %))
       (filter #(re-find #".*clj$" (.getName %))
               (file-seq (clojure.java.io/file path)))))

(defn add-templates! [cfg]
  "Look in the path specified by :templates in cfg and add files to :template-files"
  (add-classpath-and-load! (make-path (:root cfg) (:templates cfg)))
  cfg)

(defn add-plugins! [cfg]
    "Look in the path specified by :plugins in cfg and add files to :plugin-files"
    (add-classpath-and-load! (make-path (:root cfg) (:plugins cfg)))
    cfg)

(defn load-config
  ([s] (load-config (str (fs/expand-home s)) (fn [e] nil)))
  ([s f]
     (try 
       (merge (load-file s)
              {:root (-> s clojure.java.io/file .getParent)})
          (catch Exception e (f e)))))

(defn find-config []
  (loop [f (clojure.java.io/file (System/getProperty "user.dir"))]
    (if-let [cfg (load-config (str (.toString f) "/orb.clj"))]
      cfg
      (when-let [p (.getParentFile f)]
        (recur p)))))


(defn watcher [s]
  (let [d (io/file s)]
    (when (.isDirectory d)
      (let [path (.toPath d)
            ws (.. path getFileSystem newWatchService)
            wk nil]
        (.register path ws (into-array java.nio.file.WatchEvent$Kind [StandardWatchEventKinds/ENTRY_CREATE StandardWatchEventKinds/ENTRY_DELETE StandardWatchEventKinds/ENTRY_MODIFY]))        
        ))))



