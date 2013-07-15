(ns orb.core
  (:require [orb.file :as of]
            [orb.generate :as gen]
            [orb.serve :as srv ])
  (:use [clojure.tools.cli :only [cli]])
  (:gen-class))

(defn publish [cfg]
  "Using cfg, run through the steps of publishing the site"
  (-> cfg
      of/fix-directories
      of/add-sources
      of/add-templates!
      of/add-plugins!
      gen/generate))

(defn parse-args [args]
  "Process args into the specific settings for the blog and return an
   initial configuration map"
  (cli args
       ["-h" "--help" "Help"                          :flag true]
       ["-c" "--configuration" "Configuration file"   :default nil]
       ["-a" "--[no-]auto" "Automatically regenerate" :default nil]
       ["-H" "--[no-]server" "Setup local server"     :default nil]
       ["-p" "--port" "Local port (for server)"       :parse-fn #(Integer. %)]
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
    (if (:help cfg)
      (println msg))
    (publish (fix-config cfg))))
