(defproject orb "0.1.0"
  :description "Another static site generator"
  :url "http://github.com/bnbeckwith/orb"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.bnbeckwith/orgmode "0.7.4"]
                 [clj-rss "0.1.7"]
                 [me.raynes/fs "1.4.0"]
                 [me.raynes/laser "1.1.1"]
                 [compojure "1.1.5"]
                 [ring "1.2.0"]
                 [com.cemerick/pomegranate "0.2.0"]
                 [prismatic/plumbing "0.1.1"]
                 [org.clojure/tools.cli "0.2.4"]]
  :main orb.core)
