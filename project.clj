(defproject orb "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.bnbeckwith/orgmode "0.7.3"]
                 [enlive "1.1.1"]
                 [clj-rss "0.1.2"]
                 [me.raynes/fs "1.4.0"]
                 [compojure "1.1.5"]
                 [ring "1.2.0"]
                 [com.cemerick/pomegranate "0.2.0"]
                 [prismatic/plumbing "0.1.0"]
                 [org.clojure/tools.cli "0.2.4"]]
  :main orb.core)
