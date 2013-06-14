(ns orb.core
  (:require [orb.file :as of]
            [orb.generate :as gen]
            [orb.convert :as cvt])
  (:use [clojure.tools.cli :only [cli]])
  (:gen-class))

;; Tasks
;;  gen(erate)
;;  auto(-generate)
;;  serve (implies auto-generate)

;; Flow
;;  Read config
;;   - Know paths for files/output
;;   - Know paths for templates
;;   - Know paths for plugins
;;  Read tags for files
;; 

;; Of interest
;;  Strings
;;    .beginsWith (for comparing/cutting paths)
;;  Path
;;    .relativize
;;    .toAbsolutePath

(defn publish [cfg]
  "Using cfg, run through the steps of publishing the site"
  (-> cfg
      of/add-sources
      of/add-templates!
      of/add-plugins!
      cvt/convert
      gen/generate))

(defn gen-cfg [args]
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

(defn -main
  "Main entry point"
  [& args]
  (let [[cfg extra msg] (gen-cfg args)]
    (if (:help cfg)
      (println msg))
    (publish
     (merge cfg
            (if-let [f (:configuration cfg)]
              (of/load-config f)
              (of/find-config))
            (when (not (nil? (:auto cfg)))
              {:auto (:auto cfg)})
            (when (not (nil? (:server cfg)))
              {:server (:server cfg)})
            (when (nil? (:title cfg))
              {:title (:site cfg)})
            (when (nil? (:description cfg))
              {:description (:site cfg)})))))
  


