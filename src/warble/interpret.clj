(ns warble.interpret
  (:require [warble.lexer :as lexer]))

(def default-tempo 120)
(def default-time-signature (/ 4 4))
(def default-scale "C2 Major")

(def powers-of-two (iterate (partial * 2) 1))

(defn validate
  ; determine if variable assignments make sense. support hoisting.
  ; determine if beats/pairs align with defined tempo (simple base/modulus comparison should do the trick)
  ; ensure that keywords are invoked with valid arguments
  ; @context will contain meta information describing the current context of the AST traversal, such as the current TEMPO
  ; FIXME: ensure validate isn't getting recursively called too often (add a `print` under the first `let`)
  ; FIXME: use htps://clojuredocs.org/clojure.walk/walk instead of doseq
  [ast context]
  (let [vars (get context :vars {})]
    (letfn [(track-variable [label, value] (assoc context :vars (conj vars [label value])))]
      (doseq [node ast]
        (let [next-node (-> ast next first) go-next? (vector? next-node)]
          (when go-next?
            (case node
              :assign
                (validate next-node (track-variable next-node (next next-node)))
              :identifier
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
