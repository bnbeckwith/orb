(ns orb.core
  (:require [orb.file     :as of]
            [orb.generate :as gen]
            [orb.serve    :as srv]
            [orb.convert  :as cvt]
            [orb.template :as tpl]
            [orb.debug    :as dbg]
            [orb.config   :as cfg]
            [plumbing.graph :as graph])
  (:use [clojure.tools.cli :only [cli]]
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
   :get-url  of/geturlfn
   :elements (fnk [conversions get-url]
                  (map #(merge {:url (get-url (:file %))} %)
                       conversions))
   :sitemeta (fnk [title description baseurl to from root]
                  {:title title
                   :description description
                   :baseurl baseurl
                   :to to
                   :from from})
   :genfuncs (fnk [conversions destinations]
                  (for [e conversions]
                    (let [f (.toString (:file e))
                          dst (destinations f)]
                      (letfn [(mkfile []
                                (gen/gen-file! f (merge e {:name dst})))]
                        mkfile))))
   :serverstop (fnk [server port to]
                    (when server
                      (let [s (srv/serve to port)]
                        #(srv/stop-server s))))
   :regenprocess (fnk [from] nil)
   :publish      gen/publish-site
   })
  

(defn publish 
  ([h] (publish h defaultflow))
  ([h f] (publish h f graph/lazy-compile))
  ([h f m] 
     (let [plumb ((m f) h)]
       (:publish plumb))))

       ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
       ;; (binding [cfg/*siteconfig* (merge h (:sitemeta plumb))]    ;;
       ;;   ((graph/lazy-compile publishflow) (:elements plumb)))))) ;;
       ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
       ["-G" "--graph" "Print out graph debug"        :default nil]
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
  (let [[config extra msg] (parse-args args)]
    (when (:help config)
      (println msg)
      (System/exit 0))
    (when (= "default" (:graph config))
      (println (dbg/dot-body defaultflow)))
    (when (= "publish" (:graph config))
      (println (dbg/dot-body gen/publishflow)))
;;    (try
    (publish (fix-config config))
      ;; (catch Exception e 
      ;;   (println (str "Error: " (str (.getMethodName (.getStackTrace e))) "\n" msg))))))
    ))
;;)
