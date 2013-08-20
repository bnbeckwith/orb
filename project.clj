(defproject orb "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.0"]
                 [com.bnbeckwith/orgmode "0.7.0"]
                 [enlive "1.1.1"]
                 [clj-rss "0.1.3"]
                 [me.raynes/fs "1.4.4"]
                 [compojure "1.1.5"]
                 [ring "1.2.0"]
                 [prismatic/plumbing "0.1.0"]
                 [org.clojure/tools.cli "0.2.4"]]
  :main orb.core)


; (def c (-main "-c" "/home/bnbeckwith/work/projects/bnbeckwith.com/orb.clj"))
