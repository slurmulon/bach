(defproject bach "2.0.0-SNAPSHOT"
  :description "Semantic music notation"
  :url "https://github.com/slurmulon/bach"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.34"]
                 [org.clojure/data.json "0.2.6"]
                 ; [org.clojure/tools.cli "0.3.5"]
                 [org.clojure/tools.cli "1.0.194"]
                 ; [org.clojure/tools.reader "0.10.0"]
                 ; [org.clojure/tools.reader "1.3.4"]
                 ; [org.clojure/tools.reader "1.3.2"]
                 [instaparse "1.4.10"]]
  :main ^:skip-aot bach.cli
  :target-path "target/%s"
  :plugins [[lein-bin "0.3.5"]
            [lein-cljfmt "0.7.0"]
            [lein-cljsbuild "1.1.8"]]
            ; [cljsee "0.1.1-SNAPSHOT"]]
            ; [cljsee "0.1.0"]]

  :cljsbuild {:builds [{:id "prod"
                        :source-paths ["src"]
                        :modules {:bach {:exports {track bach.track}}}
                        :compiler {:main bach.core
                                   :output-to "package/index.js"
                                   :target :nodejs
                                   :output-dir "target"
                                   ;; :externs ["externs.js"]
                                   :optimizations :advanced
                                   :pretty-print true
                                   :parallel-build true}}]}

  ; ORIG
  ; :cljsbuild {
    ; :builds [{
    ;     ; The path to the top-level ClojureScript source directory:
    ;     :source-paths ["src"]
    ;     ; The standard ClojureScript compiler options:
    ;     ; (See the ClojureScript compiler documentation for details.)
    ;     :compiler {
    ;       ; :output-to "war/javascripts/main.js"  ; default: target/cljsbuild-main.js
    ;       ; :target :nodejs
    ;       ; :optimizations :none
    ;       ; :source-map true
    ;       :optimizations :whitespace
    ;       :pretty-print true}}]}

  ; :prep-tasks [["cljsee" "once"]]
  ; :cljsee {:builds [{:source-paths ["src/"]
  ;                    :output-path "target/classes"
  ;                    :rules :clj}
  ;                   {:source-paths ["src/"]
  ;                    :output-path "target/generated/cljs"
  ;                    :rules :cljs}]}
  ; :cljsbuild {:builds [{:id "none"
  ;                       :source-paths ["src/"]
  ;                       :compiler {:output-to "target/js/none.js"
  ;                                  :optimizations :none
  ;                                  :pretty-print true}}]}
  :profiles {:uberjar {:aot :all}})
