(ns warble.lexer
  (:require [instaparse.core :as insta]))

(def parse
  (insta/parser (slurp (str (System/getProperty "user.dir") "/grammar.bnf"))))
