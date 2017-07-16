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
            (create-variable [label value]
              (swap! context assoc :vars (conj (variables) [label value])))]
      (scope variables create-variable context))))

(defn validate
  [track]
  (variable-stack (fn [variables create-variable _]
    (insta/transform
      {:assign (fn [label-token value-token]
                 (let [[& label] label-token
                       [& value] value-token
                       [value-type] value-token]
                   (case value-type
                     :identifier
                       (when (not (contains? (variables) value))
                         (throw (Exception. (str "variable is not declared before it's used: " value ", " (variables)))))
                     (create-variable label value))))
      :div (fn [top-token bottom-token]
              (let [top    (-> top-token    last read-string)
                    bottom (-> bottom-token last read-string)]
                (cond
                  (not (some #{bottom} (take 10 powers-of-two)))
                    (throw (Exception. "note divisors must be base 2 and no greater than 512"))
                  (> top bottom)
                    (throw (Exception. "numerator cannot be greater than denominator")))))
      :tempo (fn [& tempo-token]
                (let [tempo (-> tempo-token last read-string)]
                  (when (not (<= 0 tempo 256))
                    (throw (Exception. "tempos must be between 0 and 256 beats per minute"))))) }
      track)))
  true)

(def validate-memo (memoize validate))

(defn provision
  ; ensures that all required elements are called at the beginning of the track with default values
  ; TimeSig, Tempo, Scale (essentially used as Key)
  [track])

; (defn cyclic? [ast])
; (defn infinite? [ast])

(defn get-tempo
  [track]
  (variable-stack (fn [& context]
    (insta/transform
      {:meta (fn [meta-token]
               (if (= meta-token "Tempo")
                 (swap! context assoc :tempo meta-token)))}
      track)
    (get @context :tempo (default-tempo)))))

(defn get-time-signature
  [track]
  (variable-stack (fn [& context]
    (insta/transform
      {:meta (fn [meta-token]
               (if (= meta-token "Time")
                 (swap! context assoc :time-signature meta-token)))}
      track)
    (get @context :time-signature (default-time-signature)))))

(defn get-lowest-beat
  [track]
  (variable-stack (fn [& context]
    (insta/transform
      ; NOTE: might need to "evaluate" duration (e.g. if it's like `1+1/2`
      {:pair (fn [duration]
               (let [lowest-duration (get @context :lowest-duration 1)]
                 (if (duration < lowest-duration)
                   (swap! context assoc :lowest-duration duration))))}
      track)
    (get @context :lowest-duration 1))))

(defn get-beats-per-measure [track] (numerator (get-time-signature)))

; TODO: convert-values
; replaces "1" with 1, "'string'" with "string", etc.

(defn dereference-variables
  [track]
  (variable-stack (fn [variables track-variable _]
    (insta/transform
      {:assign (fn [label-token value-token]
                 (let [[& label] label-token
                       [& value] value-token
                       [value-type] value-token]
                   (case value-type
                     :identifier
                       (let [stack-value (get (variables) value)]
                         [:assign label-token stack-value])
                     (do (track-variable label value-token)
                       [:assign label-token value-token]))))}
      track))))

(defn denormalize-beats
  ; this one is tricky
  ; replace any instance of a list (but not destructured list assignment) with beat tuples,
  ; where the beat equals the 1th element of the list
  ; warn on any beat list that exceeds a single measure per the time signature
  ; ---
  ; 1. compare each :pair in a :list with the :pair before it, determining how long the note is played
  ; 2. replace each :list with an equal number of elements that can be easily iterated through at a constant rate
  [track]
  (if (validate track)
    (variable-stack (fn [& context]
      ; first find the lowest common beat
      (let [lowest-beat (get-lowest-beat track)
            beats-per-measure 4
            beat-type (/ 1 lowest-beat)
            time-sig (get-time-signature)
            deref-track (dereference-variables track)]
        (insta/transform
          {:pair (fn [duration notes]
                    (let []
                      ))}
          deref-track)))

      ; then transform the :pairs into slices based on the lowest common beat
      ; NOTE: may also want to consider the tempo here, will help minimize the efforts
      ; of the high-level player
      ; Example: 4/4 timesig, lowest 1 (whole note) = 1 element per measure
      ; Example: 4/4 timesig, lowest 1/2 (half note) = 2 elements per measure
      ; Example: 4/4 timesig, lowest 1/4 (quarter note) = 4 elements per measure
      ; Example: 3/4 timesig, lowest 1/2 (half note) = 1.5 elements per measure (?)
      ; Example: 3/4 timesig, lowest 1/4 (quarter note) = 3 elements per measure
      )))

(defn denormalize-measures
  ; given a slice size (number of measures per slice), returns a periodic sliced list of equaly sized measures that
  ; can be stepped through sequentially (adds a sense of 1st measure, 2nd measure, etc.)
  [tree slice-size])

(defn denormalize
  ; processes an AST and returns a denormalized version of it that contains all the information necessary to interpet a track in a single stream of data (no references, all resolved values).
  ; normalize-variables
  ; normalize-beats
  [tree])
