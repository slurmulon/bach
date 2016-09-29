(ns warble.interpret
  (:require [warble.lexer :as lexer]))

(def default-tempo 120)
(def default-time-signature (4/4))
(def default-scale "C2 Major")

(defn validate
  ; determine if variable assignments make sense. support hoisting.
  ; determine if beats/pairs align with defined tempo (simple base/modulus comparison should do the trick)
  [ast])

(defn provision
  ; ensures that all required elements are called at the beginning of the track with default values
  ; TimeSignature, Tempo, Scale (essentially used as Key)
  [ast])

(defn cyclic? [ast])
(defn infinite? [ast])

(defn normalize-variables
  ; replaces variable references with their associated data
  ; support hoisting!
  [ast])

(defn normalize-beats
  ; replace any instance of a list (but not destructured list assignment) with beat tuples,
  ; where the beat equals the 1th element of the list
  ; warn on any beat list that exceeds a single measure per the time signature
  [ast])

(defn normalize-measures
  ; given a slice size (number of measures per slice), returns a periodic sliced list of equaly sized measures that
  ; can be stepped through sequentially (adds a sense of 1st measure, 2nd measure, etc.)
  [ast slice-size])

(defn normalize
  ; processes an AST and returns a normalized version of it that contains all the information necessary to interpet a track.
  ; normalize-variables
  ; normalize-beats
  [ast])
