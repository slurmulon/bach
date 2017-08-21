(ns warble.lexer
  (:require [instaparse.core :as insta]
            [clojure.java.io :as io]))

; (def parse
;   (insta/parser (slurp (str (System/getProperty "user.dir") "/grammar.bnf"))))

(def parse
  (insta/parser (->> "grammar.bnf" io/resource slurp)))
