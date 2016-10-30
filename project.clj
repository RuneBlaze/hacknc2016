(defproject ramp-example "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.taoensso/tufte "1.0.2"]
                 [ramp "0.1.0-SNAPSHOT"]
                 [org.clojure/tools.nrepl "0.2.12"]]
  :main ^:skip-aot ramp-example.core
  :target-path "target/%s"
  :jvm-opts ["-XstartOnFirstThread"]
  :profiles {:uberjar {:aot :all}})
