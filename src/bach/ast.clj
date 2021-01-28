(ns bach.ast
  (:require [instaparse.core :as insta]
            [clojure.java.io :as io]))
  ; #?(:cljs [instaparse.core :as insta]
  ;    :clj  [instaparse.core :as insta :refer [defparser]]))

(def parse
  (insta/parser (->> "grammar.bnf" io/resource slurp)))
