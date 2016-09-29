(ns warble.lexer
  (:require [instaparse.core :as insta]))

(def parse
  (insta/parser (slurp "grammar.bnf")))
