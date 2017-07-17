; TODO: rename to `parse`

; http://xahlee.info/clojure/clojure_instaparse.html
; http://xahlee.info/clojure/clojure_instaparse_transform.html

; (ns warble.interpret
;   (:require [warble.lexer :as lexer]
;             [instaparse.core :as insta]))

(ns warble.interpret
  (:require [instaparse.core :as insta]))

(defn ratio-to-vector [ratio]
  ((juxt numerator denominator) ratio))

(def default-tempo 120)
(def default-scale "C2 Major")
(def default-time-signature [4 4])
; (def default-time-signature (ratio-to-vector (/ 4 4))
; (def default-time-signature {:numerator 4 :denominator 4})

(def powers-of-two (iterate (partial * 2) 1))

(defn variable-stack
  [scope]
  (let [context (atom {})]
    (letfn [(variables []
              (get @context :vars {}))
            ; TODO: might just want to move this into `dereference-variables`
            (create-variable [label value]
              (swap! context assoc :vars (conj (variables) [label value])))]
      (scope variables create-variable context))))

; TODO: integreate reduce-values
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
                    (throw (Exception. "tempos must be between 0 and 256 beats per minute")))))}
      track)))
  true)

(def validate-memo (memoize validate))

(defn reduce-values
  [track]
  (insta/transform
    {:add +, :sub -, :mul *, :div /
    :number clojure.edn/read-string
    :string #(clojure.string/replace % #"^(\"|\')|(\"|\')$" "")} track))

(defn provision
  ; ensures that all required elements are called at the beginning of the track with default values
  ; TimeSig, Tempo, Scale (essentially used as Key)
  ; Also ensure `ms-per-beat` is easily available at a high level
  [track])

(defn get-tempo
  [track]
  (let [tempo (atom default-tempo)]
    (insta/transform
      {:meta (fn [kind value]
               (if (= kind "Tempo")
                 (reset! tempo value)))}
      track)
    @tempo))

(defn get-time-signature
  [track]
  (let [time-signature (atom default-time-signature)]
    (insta/transform
      {:meta (fn [kind value]
               (if (= kind "Time")
                 (reset! time-signature value)))} ; TODO: need to ensure this ends up as a list instead of a ratio [num, denom]
      track)
    @time-signature))

(defn get-lowest-beat
  [track]
  (let [lowest-duration (atom 1)
        reduced-track (reduce-values track)]
    (insta/transform
      ; NOTE: might need to "evaluate" duration (e.g. if it's like `1+1/2`
      {:pair (fn [duration _]
               (if (< duration @lowest-duration)
                 (reset! lowest-duration duration)))}
      reduced-track)
    (min 1 @lowest-duration)))

; TODO: (defn get-number-of-beats)

(defn get-beats-in-track
  [track]
  (let [total-beats (atom 0)
        reduced-track (reduce-values track)]
    (insta/transform
      {:pair (fn [duration _]
               (swap! total-beats + duration))}
      reduced-track)
    @total-beats))

(defn get-beats-per-measure [track] (first (get-time-signature track))) ; AKA numerator

; TODO: integrate this into `provision`, that way it's easy for the high-level engine to
; iterate using `setInterval` or the like
(defn get-ms-per-beat
  [track]
  (let [beats-per-measure (get-beats-per-measure track)
        lowest-beat-size (get-lowest-beat track)
        tempo (get-tempo track)
        ms-per-measure (/ tempo beats-per-measure)] ; FIXME: this isn't right. needs to consider beats-in-track
    (println "!!!!! lowest beat size" lowest-beat-size)
    (println "!!!!! ms-per-measure" ms-per-measure)
    (float (* ms-per-measure lowest-beat-size)))) ; TODO: might need to normalize this, divide by fraction vs multiply by rational num

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
  ; 2. replace each :list with an equal number of elements that can be easily iterated through at a constant rate.
  ;    also modify each :note to include durations
  ;    return as [:measure [...]]
  [track]
  (if (validate track)
    (variable-stack (fn [& context]
      (let [lowest-beat (get-lowest-beat track)
            beats-per-measure 4
            beat-type (/ 1 lowest-beat) ; greatest is a whole note
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
  [track slice-size])

(defn denormalize
  ; processes an AST and returns a denormalized version of it that contains all the information necessary to interpet a track in a single stream of data (no references, all resolved values).
  ; normalize-variables
  ; normalize-beats
  [track])
