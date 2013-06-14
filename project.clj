(defproject orb "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.0"]
                 [com.bnbeckwith/orgmode "0.6.0"]
                 [enlive "1.1.1"]
                 [clj-rss "0.1.3"]
                 [me.raynes/fs "1.4.0"]
                 ; [ojo "1.1.0"] Watching directories (Java 7 required)
                 [bultitude "0.2.2"]
                 [com.cemerick/pomegranate "0.2.0"]
;                 [commons-io/commons-io "2.4"]
                 [clj-time "0.5.0"]
;                 [endophile "0.1.0"]  Markdown support
                 [org.apache.commons/commons-lang3 "3.1"]
                 [compojure "1.1.5"]
                 [ring "1.1.8"]
                 [org.clojure/tools.cli "0.2.2"]]
  :main orb.core)


; (def c (-main "-c" "/home/bnbeckwith/work/projects/bnbeckwith.com/orb.clj"))