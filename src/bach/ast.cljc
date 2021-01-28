(ns bach.ast
  ; (:require [instaparse.core/parser :as parser]
  ;           [clojure.java.io :as io]))

  ; (:require [instaparse.core :refer [parser]]
  ;           [bach.data :refer-macros [inline-resource]]))
  (:require [instaparse.core :refer [parser]]
            #?(:clj [bach.data :refer [inline-resource]]
               :cljs [bach.data :refer-macros [inline-resource]])))
  ; (:require
  ;   [instaparse.core :refer [parser]]
  ;   #?(:cljs (:require-macros [bach.data :refer [inline-resource]])))
  ; (:require
  ;   [instaparse.core/parser :as parser]
  ;   #?(:clj [clojure.java.io :as io]
  ;      :cljs [bach.data :refer-macros [inline-resource]])))
  ; #?(:cljs [instaparse.core :as insta]
  ;    :clj  [instaparse.core :as insta :refer [defparser]]))

(def parse
  ; (parser ->> "grammar.bnf" inline-resource))
  (parser (inline-resource "grammar.bnf")))

; #?(:clj
; (def parse
;   (parser ->> "grammar.bnf" io/resource slurp)))
; :cljs
; (defmacro parse

