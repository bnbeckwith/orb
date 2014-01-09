(ns orb.core
  (:require [orb.file :as of]
            [orb.generate :as gen]
            [orb.serve :as srv ]
            [orb.convert :as cvt]
            [orb.template :as tpl]
            [plumbing.graph :as graph])
  (:use [clojure.tools.cli :only [cli]]
        [clojure.string :only [lower-case]]
        [plumbing.core]))

(def defaultflow
  {:from (fnk [source root] (of/make-abs source root))
   :to   (fnk [output root] (of/make-abs output root))
   :tpldir (fnk [templates root] (of/make-abs templates root))
   :allfiles of/get-files
   :files (fnk [allfiles] 
                     (filter #(not (re-find #"~$" (.toString %))) allfiles))
   :conversions (fnk [files] 
                     (map 
                      #(merge {:file %} (cvt/convert %)) files))
   :destinations of/destinations
   :get-url of/geturlfn
   :elements (fnk [conversions get-url]
                  (map #(merge {:url (get-url (:file %))} %)
                       conversions))
   :sitemeta (fnk [title description baseurl]
                  {:title title
                   :description description
                   :baseurl baseurl})
   :genfuncs (fnk [conversions destinations]
                  (for [e conversions]
                    (let [f (.toString (:file e))
                          dst (destinations f)]
                      (letfn [(mkfile []
                                (gen/gen-file! f (merge e {:name dst})))]
                        mkfile))))
   :blog-entries (fnk [elements]
                      (reverse 
                       (sort-by 
                        (comp :date :attribs :conversion)
                        (filter #(= (lower-case 
                                     (get-in % [:conversion :attribs :category] "")) 
                                    "blog") 
                                elements))))
   :rss (gen/rssfn)
   :index gen/indexfn
   :blogindex gen/blogindexfn
   :serverstop (fnk [server port to]
                    (when server
                      (let [s (srv/serve to port)]
                        #(srv/stop-server s))))
   :regenprocess (fnk [from] nil)
   :tags gen/maketags
   :categories gen/makecategories
   :archive gen/makearchive
   })

(defn publish 
  ([h] (publish h defaultflow))
  ([h f] (publish h f graph/eager-compile))
  ([h f m] ((m f) h)))
     
(defn parse-args [args]
  "Process args into the specific settings for the blog and return an
   initial configuration map"
  (cli args
       ["-h" "--help" "Help"                          :flag true]
       ["-c" "--configuration" "Configuration file"   :default nil]
       ["-a" "--[no-]auto" "Automatically regenerate" :default nil]
       ["-H" "--[no-]server" "Setup local server"     :default nil]
       ["-p" "--port" "Local port (for server)"       :parse-fn #(Integer. %) :default 8888]
       ["-r" "--root" "Root location"                 :default (System/getProperty "user.dir")]
       ["-b" "--baseurl" "Base url"                   :default "/"]
       ["-S" "--site" "Site url (e.g. example.com)"   :default "localhost"]
       ["-s" "--source" "Source directory"            :default "source"]
       ["-t" "--templates" "Template directory"       :default "templates"]
       ["-l" "--plugins" "Plugin directory"           :default "plugins"]
       ["-o" "--output" "Output directory"            :default "site"]
       ["-t" "--title" "Site title"                   :default nil]
       ["-d" "--description" "Site description"       :default nil]))

(defn fix-config [cfg']
  (let [cfg (merge cfg'
                    (if-let [f (:configuration cfg')]
                      (of/load-config f)
                      (of/find-config)))]
    (merge cfg
           (when (nil? (:title cfg)) 
             {:title (:site cfg')})
           (when (nil? (:description cfg))
             {:description (:site cfg')}))))
 
(defn -main
  "Main entry point"
  [& args]
  (let [[cfg extra msg] (parse-args args)]
    (when (:help cfg)
      (println msg)
      (System/exit 0))
;;    (try
      (publish (fix-config cfg))
      ;; (catch Exception e 
      ;;   (println (str "Error: " (str (.getMethodName (.getStackTrace e))) "\n" msg))))))
))
;;)
