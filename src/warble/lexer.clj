(ns warble.lexer
  (:require [instaparse.core :as insta]))

(def tokenize
  (insta/parser (slurp "grammar.bnf")))
