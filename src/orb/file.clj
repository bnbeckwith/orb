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

(defn make-url 
  "Return a URL out of a given destination filename f and cfg"
  ([f cfg]
     (make-url f cfg (:site cfg)))
  ([f cfg pfx]
     (let [dst (:output cfg)]
       (clojure.string/replace-first f dst pfx))))

(defn change-ext 
  "Return a (base) filename of f with extension changed to x"
  [f x]
  (if (nil? x)
    f
    (if-let [ext (second (fs/split-ext f))]
      (clojure.string/replace f ext x)
      f)))

(defn to-destination [f cfg]
  "Take f and return destination directory"
  (let [sdir (:source cfg)
        ddir (:output cfg)
        fin  (.toString f)]
    (clojure.string/replace-first fin sdir ddir)))

(defn add-sources [cfg]
  "Look in the path specified by :source in cfg and add files to :source-files"
  (merge cfg
         {:source-files
          (file-seq 
           (io/file (:source cfg)))}))

(defn fix-directories [cfg]
  "Fix any root/base/destination directories to be absolute."
  (let [in (io/file (:source cfg))
        out (io/file (:output cfg))
        root (io/file (:root cfg))]
    (letfn [(make-abs [f]
              (if (.isAbsolute f)
                f
                (str (.getPath root)
                     java.io.File/separator
                     (.getName f))))]
    (merge cfg
           {:source (make-abs in)
            :output (make-abs out)}))))

(defn add-classpath-and-load! [path]
  "Add a given path to the classpath and load any clj files found"
  (pom/add-classpath path)
  (map #(load-file (.getAbsolutePath %))
       (filter #(re-find #".*clj$" (.getName %))
               (file-seq (io/file path)))))

(defn add-templates! [cfg]
  "Look in the path specified by :templates in cfg and add files to :template-files"
  (add-classpath-and-load! (make-path (:root cfg) (:templates cfg)))
  cfg)

(defn add-plugins! [cfg]
    "Look in the path specified by :plugins in cfg and add files to :plugin-files"
    (add-classpath-and-load! (make-path (:root cfg) (:plugins cfg)))
    cfg)

(defn load-config
  "Load a given configuration file"
  ([s] (load-config (str (fs/expand-home s)) (fn [e] nil)))
  ([s f]
     (try 
       (merge (load-file s)
              {:root (-> s io/file .getParent)})
          (catch Exception e (f e)))))

(defn find-config []
  "If no config is supplied, walk up the current directory path until
   one is found."
  (loop [f (io/file (System/getProperty "user.dir"))]
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



