(defproject bach "3.0.0-SNAPSHOT"
  :description "Semantic music notation"
  :url "https://github.com/slurmulon/bach"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.8.34"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/tools.cli "1.0.194"]
                 [org.clojure/core.memoize "1.0.236"]
                 [instaparse "1.4.10"]
                 [nano-id "1.0.0"]
                 [hiccup-find "1.0.0"]
                 [eftest "0.5.9"]]
  :main ^:skip-aot bach.cli
  :target-path "target/%s"
  :plugins [[lein-bin "0.3.5"]
            [lein-cljfmt "0.7.0"]
            [lein-eftest "0.5.9"]]
  :deploy-repositories [["releases" :clojars]
                         ["snapshots" :clojars]]
  :eftest {:multithread? false}
  :profiles {:uberjar {:aot :all}})
