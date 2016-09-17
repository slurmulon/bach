(ns warble.parser
  (:require [warble.lexer :refer tokenize]))

; responsible for consuming an AST and producing a linear stream of data

(defn contig-stream [ast stream]
  (loop [node ast]
    (cond (or :root :statement) (recur ))))
