(ns bach.ast
  (:require [instaparse.core :as insta]
            [clojure.java.io :as io]))

(def parse
  (insta/parser (->> "grammar.bnf" io/resource slurp)))
