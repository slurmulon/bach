; @see https://clojurescript.org/guides/javascript-modules#javascript-modules
; @see https://clojurescript.org/guides/webpack
; @see https://lambdaisland.com/blog/2017-05-02-nodejs-scripts-clojurescript
; @see https://clojureverse.org/t/generating-es-modules-browser-deno/6116
; @see https://clojurescript.org/reference/compiler-options#target
; @see https://clojurescript.org/reference/advanced-compilation#access-from-javascript ***
; @see http://webcache.googleusercontent.com/search?q=cache:TNyqrhVeNekJ:www.matthewstump.com/misc/2012/06/04/writing-nodejs-modules-in-clojurescript+&cd=3&hl=en&ct=clnk&gl=us&client=ubuntu

(ns bach.core
  (:require ;[bach.track :as track]
            ;[bach.data :as data]
            ;[bach.ast :as ast]
            [bach.track :refer [compose]]
            [clojure.string :as str]
            [cljs.nodejs :as nodejs]))

(defn ^:export square [x]
  (* x x))

(defn ^:export str-thing [x] (println "wut" x))

(def ^:export compose-track compose)

; (def ^:export track)
; (def ^:export data)
; (def ^:export ast)

; (def ^:export track bach.track)
; (def ^:export data bach.data)
; (def ^:export ast bach.ast)

; (def ^:export default {:track bach.track
;                        :data bach.data
;                        :ast bach.ast})
(defn noop [] nil)

(nodejs/enable-util-print!)

(println "[core/track]" bach.track)

; (defn -main [& args]
;   )
  ; (-> (str/join " " args)
  ;     (str/replace #"cker\b" "xor")
  ;     (str/replace #"e|E" "3")
  ;     (str/replace #"i|I" "1")
  ;     (str/replace #"o|O" "0")
  ;     (str/replace #"s|S" "5")
  ;     (str/replace #"a|A" "4")
  ;     (str/replace #"t|T" "7")
  ;     (str/replace #"b|B" "6")
  ;     (str/replace #"c|C" "(")
  ;     println))

; (set! *main-cli-fn* -main)
(set! *main-cli-fn* noop)
