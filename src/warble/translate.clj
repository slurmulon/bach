(ns warble.parser
  (:require [warble.lexer :refer tokenize]))

; responsible for consuming an AST of nodes and producing a linear stream of data

; TODO: should result with:
; beat -> [notes]*
; AKA sliced-stream..?
(defn continous-stream [nodes stream]
  (loop [node nodes] ; TODO: since pattern is "loop through arg1 and arg2 (node1 and node2), should prob use a reducer
    (cond (or :root :statement) (recur [node] (conj stream node)))))

; TODO: fastest-beat (lowest-common-beat)
