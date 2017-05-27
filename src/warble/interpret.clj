(ns warble.interpret
  (:require [warble.lexer :as lexer]))

(def default-tempo 120)
(def default-time-signature (/ 4 4))
(def default-scale "C2 Major")

(def powers-of-two (iterate (partial * 2) 1))

(defn validate
  [ast context]
  (println "AST" ast)
  (let [vars (get context :vars {})]
    (letfn [(track-variable [label, value] (assoc context :vars (conj vars [label value])))]
      (doseq [node ast]
        (println "~node" node (vector? node))
        (if (vector? node)
          (let [token (first node) next-node (-> node rest first)]
            (println "--- token" token)
            (println "--- node" node)
            (println "--- next-node ->>>>> " next-node)
            (case token
              ; :statement ; FIXME: remove need for this, should just fall to case default
              ;   (println "STATEMENT" next-node)
              :assign
                (println "ASSIGN" next-node)
              ; (:statement ; FIXME: remove need for this, should just fall to case default
              ;   (println "STATEMENT" next-node))
              ; (:assign
              ;   (println "ASSIGN" next-node))
              (validate next-node context))))))
        ; (cond
        ;   (seq? node) ()

  true))


(defn validate-BAK
  ; determine if variable assignments make sense. support hoisting.
  ; determine if beats/pairs align with defined tempo (simple base/modulus comparison should do the trick)
  ; ensure that keywords are invoked with valid arguments
  ; @context will contain meta information describing the current context of the AST traversal, such as the current TEMPO
  ; FIXME: ensure validate isn't getting recursively called too often (add a `print` under the first `let`)
  ; FIXME: use htps://clojuredocs.org/clojure.walk/walk instead of doseq
  [ast context]
  (println "\nRecursing validate" ast)
  (println "\trest ast" (next ast))
  (let [vars (get context :vars {})]
    (letfn [(track-variable [label, value] (assoc context :vars (conj vars [label value])))]
      (doseq [node ast]
        (println "ast" ast)
        ; TODO might need to revisit the vars here (TEST BELOW EXPRESSION)
        ; (let [next-node (-> ast rest first) go-next? (vector? next-node)]
        (let [next-node (-> ast next first) go-next? (vector? next-node)] ; true is just a test
          (when go-next?
            (println "node, next-node" node next-node)
            (case node
              :assign
                (do
                  (let [assignment (rest ast) assign-label (next next-node) assign-value (last assignment)]
                    (println "Tracking variable" next-node (next next-node))
                    (println "--- rest!" assign-label assign-value)
                    ; (println "--- rest" (rest ast) (-> ast rest first) (-> ast rest last))
                    ; (validate next-node (track-variable next-node (next next-node)))))
                    ;(validate next-node (track-variable assign-label assign-value)))) ; SHOULD WORK (sort of)
                    (validate next-node context))) ; EXPERIMENT
              :identifier ; FIXME: needs to check for [:identifier :value], something like that
                (let [has-var (contains? vars next-node)]
                  (cond (has-var) (validate next-node context) ; known variable, keep going
                        (not has-var) (validate next-node (track-variable next-node :empty)) ; register unknown variable
                        (and (not (next ast)) (not (contains? context :vars))) (throw (Exception. "variable is never declared"))))
              :pair (if (contains? (take 10 powers-of-two) next-node)
                        (validate next-node context)
                        (throw (Exception. "note divisors must be base 2 and no greater than 512")))
              :tempo (if (<= 0 next-node 256)
                        (validate next-node context)
                        (throw (Exception. "tempos must be between 0 and 256 beats per minute")))
              (validate next-node context))))))
      true))


(defn provision
  ; ensures that all required elements are called at the beginning of the track with default values
  ; TimeSig, Tempo, Scale (essentially used as Key)
  [ast])

(defn cyclic? [ast])
(defn infinite? [ast])

(defn denormalize-variables
  ; replaces variable references with their associated data
  ; support hoisting!
  [ast])

(defn denormalize-beats
  ; replace any instance of a list (but not destructured list assignment) with beat tuples,
  ; where the beat equals the 1th element of the list
  ; warn on any beat list that exceeds a single measure per the time signature
  [ast])

(defn denormalize-measures
  ; given a slice size (number of measures per slice), returns a periodic sliced list of equaly sized measures that
  ; can be stepped through sequentially (adds a sense of 1st measure, 2nd measure, etc.)
  [ast slice-size])

(defn denormalize
  ; processes an AST and returns a denormalized version of it that contains all the information necessary to interpet a track in a single stream of data (no references, all resolved values).
  ; normalize-variables
  ; normalize-beats
  [ast])
