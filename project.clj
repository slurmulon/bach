(defproject bach "0.3.0-SNAPSHOT"
  :description "Notation for musical loops and tracks with a focus on readability and productivity"
  :url "https://github.com/slurmulon/bach"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/tools.cli "0.3.5"]
                 [instaparse "1.4.3"]]
  :main ^:skip-aot bach.cli
  :target-path "target/%s"
  :plugins [[lein-bin "0.3.5"]]
  :profiles {:uberjar {:aot :all}})
