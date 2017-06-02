; http://xahlee.info/clojure/clojure_instaparse.html
; http://xahlee.info/clojure/clojure_instaparse_transform.html

; (ns warble.interpret
;   (:require [warble.lexer :as lexer]
;             [instaparse.core :as insta]))

(ns warble.interpret
  (:require [instaparse.core :as insta]))

(def default-tempo 120)
(def default-time-signature (/ 4 4))
(def default-scale "C2 Major")

(def powers-of-two (iterate (partial * 2) 1))

; @see: instaparse.core/transform (http://xahlee.info/clojure/clojure_instaparse_transform.html
(defn validate
  [tree]
  (let [context (atom {})]
    (letfn [(variables [] (get @context :vars {}))
            (track-variable [label value] (swap! context assoc :vars (conj (variables) [label value])))]
      (insta/transform
        {:assign (fn [left right]
                   ; (println "label, value" left right)
                   (let [label (last left)
                         value (last right)
                         value-type (first right)]
                     (case value-type
                       :identifier
                         (when-let [unknown-var (not (contains? (variables) value))]
                           (throw (Exception. "variable is not declared before it's used")))
                        (track-variable label value))))
         ; TODO: :pair
         ; TODO: :add
         :div (fn [left right] ; AKA "tuple"
                (let [top    (-> left  last read-string)
                      bottom (-> right last read-string)]
                 ; (println "!!!div" top bottom (type top) (type bottom)) ))
                 (when (not (some #{bottom} (take 10 powers-of-two))) ; FIXME: value here doesn't make sense
                   (throw (Exception. "note divisors must be base 2 and no greater than 512")))))
         :tempo (fn [& value]
                  (let [tempo (last value)]
                    (when (not (<= 0 tempo 256))
                      (throw (Exception. "tempos must be between 0 and 256 beats per minute"))))) }
      tree))
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
