; TODO: rename to `parse`

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

(defn variable-stack
  [scope]
  (let [context (atom {})]
    (letfn [(variables []
              (get @context :vars {}))
            (track-variable [label value]
              (swap! context assoc :vars (conj (variables) [label value])))]
      (scope variables track-variable context))))

(defn validate
  [tree]
  (variable-stack (fn [variables track-variable context]
    (insta/transform
      {:assign (fn [label-token value-token]
                 (let [label (last label-token)
                       value (last value-token)
                       value-type (first value-token)]
                   (case value-type
                     :identifier
                       (when (not (contains? (variables) value))
                         (throw (Exception. "variable is not declared before it's used")))
                     (track-variable label value))))
       ; TODO: :pair (tuple)
       ; TODO: :add (not sure there's much to validate, really)
       :div (fn [top-token bottom-token]
              (let [top    (-> top-token    last read-string)
                    bottom (-> bottom-token last read-string)]
                (cond
                  (not (some #{bottom} (take 10 powers-of-two)))
                    (throw (Exception. "note divisors must be base 2 and no greater than 512"))
                  (> top bottom)
                    (throw (Exception. "numerator cannot be greater than denominator")))))
       :tempo (fn [& right]
                (let [tempo (-> right last read-string)]
                  (when (not (<= 0 tempo 256))
                    (throw (Exception. "tempos must be between 0 and 256 beats per minute"))))) }
    tree)))
  true)

(defn provision
  ; ensures that all required elements are called at the beginning of the track with default values
  ; TimeSig, Tempo, Scale (essentially used as Key)
  [tree context])

; (defn cyclic? [ast])
; (defn infinite? [ast])

(defn denormalize-variables
  [tree]
  (if (validate tree)
    (variable-stack (fn [get-variables track-variable context]
      (insta/transform
        {:assign (fn [label-token value-token]
                    (let [label (last label-token)
                          value (last value-token)
                          value-type (first value-token)]
                      (case value-type
                        :identifier
                          (let [stack-value (get (get-variables) value)]
                            [:assign [:identifier label] stack-value])
                        (do (track-variable label value-token)
                          [:assign label-token value-token]))))}
        tree)))))

(defn denormalize-beats
  ; replace any instance of a list (but not destructured list assignment) with beat tuples,
  ; where the beat equals the 1th element of the list
  ; warn on any beat list that exceeds a single measure per the time signature
  [tree]
  (if (validate tree)
    (variable-stack (fn [get-variables track-variable context]
      (insta/transform
        {:list (fn [label-token value-token] [])})))))

(defn denormalize-measures
  ; given a slice size (number of measures per slice), returns a periodic sliced list of equaly sized measures that
  ; can be stepped through sequentially (adds a sense of 1st measure, 2nd measure, etc.)
  [tree slice-size])

(defn denormalize
  ; processes an AST and returns a denormalized version of it that contains all the information necessary to interpet a track in a single stream of data (no references, all resolved values).
  ; normalize-variables
  ; normalize-beats
  [tree])
