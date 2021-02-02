; @see https://clojurescript.org/guides/javascript-modules#javascript-modules
; @see https://clojurescript.org/guides/webpack
; @see https://lambdaisland.com/blog/2017-05-02-nodejs-scripts-clojurescript
; @see https://clojureverse.org/t/generating-es-modules-browser-deno/6116
; @see https://clojurescript.org/reference/compiler-options#target
; @see https://clojurescript.org/reference/advanced-compilation#access-from-javascript ***
; @see http://webcache.googleusercontent.com/search?q=cache:TNyqrhVeNekJ:www.matthewstump.com/misc/2012/06/04/writing-nodejs-modules-in-clojurescript+&cd=3&hl=en&ct=clnk&gl=us&client=ubuntu

(ns bach.core
  ; (:require [bach.track :as track]
  ;           [bach.data :as data]
  ;           [bach.ast :as ast]
  (:require [bach.track :as track]
            ; [bach.track]
            [clojure.string :as str]
            [cljs.nodejs :as nodejs]))

(def ^:export compose track/compose)
; (def ^:export parse ast/parse)

(defn noop [] nil)

(nodejs/enable-util-print!)

(set! *main-cli-fn* noop)
