; TODO: rename to `parse`, or even `track`

; http://xahlee.info/clojure/clojure_instaparse.html
; http://xahlee.info/clojure/clojure_instaparse_transform.html

; (ns warble.interpret
;   (:require [warble.lexer :as lexer]
;             [instaparse.core :as insta]))

(ns warble.interpret
  (:require [instaparse.core :as insta]))

; (defn ratio-to-vector [ratio]
;   ((juxt numerator denominator) ratio))

(declare dereference-variables reduce-track) ; TODO: mention every method here so they are hoisted and declaration order becomes irrelevant

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

(defn reduce-track
  [track]
  (-> track
     dereference-variables
     reduce-values))

(defn provision
  ; ensures that all required elements are called at the beginning of the track with default values
  ; TimeSig, Tempo, Scale (essentially used as Key)
  ; Also ensure `ms-per-beat`, `lowest-beat` and `total-beats` is easily available at a high level
  [track])

; TODO: provision-meta

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

(defn get-beats-per-measure
  [track]
  (first (get-time-signature track))) ; AKA numerator

(defn get-total-beats
  [track]
  (let [total-beats (atom 0)
        reduced-track (reduce-values track)]
    (insta/transform
      {:pair (fn [duration _]
               (swap! total-beats + duration))}
      reduced-track)
    @total-beats))

(defn get-total-measures
  [track]
  (let [total-beats (get-total-beats track)
        beats-per-measure (get-beats-per-measure track)
        adjusted-total-measures (if (< 1 total-beats) (/ total-beats beats-per-measure) total-beats)]
    adjusted-total-measures))

(defn get-normalized-total-beats
  ; based on total-measures, which is adjusted to ensure at least one measure is played
  [track]
  (let [beats-per-measure (get-beats-per-measure track)
        total-measures (get-total-measures track)
        total-beats (* total-measures beats-per-measure)]
    total-beats))

; NOTE: this really belongs at a higher-level, in the track engine, but can be useful for providing default durations
; FIXME: make the minimum duration at least 1 measure (starts at total-beats, needs to be considered there as well
; - answer is likely in making this based on `get-totalmeasures` instead of `get-total-beats`
(defn get-total-duration
  [track unit]
  (let [total-beats (get-normalized-total-beats track)
        tempo-bpm (get-tempo track)
        duration-minutes (/ total-beats tempo-bpm)
        duration-seconds (* duration-minutes 60)
        duration-milliseconds (* duration-seconds 1000)]
    (case unit
      :milliseconds duration-milliseconds
      :seconds duration-seconds
      :minutes duration-minutes)))

; TODO: integrate this into `provision`, that way it's easy for the high-level engine to
; iterate using `setInterval` or the like
(defn get-ms-per-beat
  [track]
  (let [beats-per-measure (get-beats-per-measure track)
        lowest-beat-size (get-lowest-beat track)
        tempo (get-tempo track)
        total-measures (get-total-measures track)
        total-duration-ms (get-total-duration track :milliseconds)
        ms-per-measure (/ total-duration-ms total-measures)]
    (float (* ms-per-measure lowest-beat-size))))

; NOTE: can probably just move this into denormalize-measures or denormalize-beats (probably don't need both)
(defn explode-pair-into-measures
  [track pair]
  (let [;current-measure (atom [])
        ;beat-cursor (atom 0)
        lowest-beat (get-lowest-beat track)
        num-beats-per-measure (get-beats-per-measure track) ; 4
        num-measures-in-pair (first pair) ; AKA num-beats-in-pair AKA index-of-pair
        ; TODO: indices-of-pair [measure-index, beat-index]
        atoms-in-pair (last pair)
        exploded-measures (make-array Void/TYPE num-measures-in-pair num-beats-per-measure)] ; essentially the same as beats-in-pair
        ; beats-in-pair (first pair) ; 2 (measures, so 8 beats)
        ; measures-in-pair (if (> beats-in-pair 1) () 1)]
    ; for each atom in pair, add the duration (aka `beats-in-pair`) to it's list of `:arguments` ([:duration beats-in-pair])
    ; (if (< 1 num-measures-in-pair)
    ;   (
    ))

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

; (defn compile-notes
;   [duration notes]
;   {:duration duration :notes notes}) 

; TODO: rename to either:
; - reduce-beats
; - compile-beats
; - normalize-beats (it's really the opposite now that I've worked through it)
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
  (let [total-measures (get-total-measures track)
        total-beats (get-total-beats track)
        beat-cursor (atom 0) ; NOTE: measured in whole notes, not the lowest beat! (makes parsing easier)
        ; lowest-beat (get-lowest-beat track)
        beats-per-measure (get-beats-per-measure track)
        ; beat-type (/ 1 lowest-beat) ; greatest is a whole note
        ; time-sig (get-time-signature)
        ; measures (atom (vec (make-array Void/TYPE total-measures beats-per-measure)))
        ; measures (atom (vec (make-array clojure.lang.PersistentArrayMap total-measures beats-per-measure)))
        measures (atom (mapv #(into [] %) (make-array clojure.lang.PersistentArrayMap total-measures beats-per-measure)))
        reduced-track (reduce-track track)]
    (letfn [(update-measures [measure-index beat-index notes]
              (println "updating measures! (mi, bi, notes)" measure-index beat-index notes)
              (swap! measures assoc-in [measure-index beat-index] notes))
              ; (swap! measures update-in [measure-index beat-index] notes))
            (beat-indices [beat]
              (println "beat-indices [beat]" beat)
              (let [global-beat-index (+ @beat-cursor beat)
                    local-beat-index (mod global-beat-index beats-per-measure)
                    measure-index (int (Math/floor (float (/ global-beat-index beats-per-measure))))]
                    ; measure-index (Math/ceil (/ (+ beat-cursor beat) measures))]
                {:measure measure-index :beat local-beat-index}))]
      (insta/transform
        {:pair (fn [beats notes]
                 (println "denorm-beats beats" beats)
                 (println "denorm-beats notes" notes)
                 (let [indices (beat-indices beats)
                       measure-index (:measure indices)
                       beat-index (:beat indices)
                       compiled-notes {:duration beats :notes notes}]
                  (println "--- current measures" @measures) 
                  ; TODO: some other stuff, mostly building/filling the `measures` array
                  ; TODO: add duration to every element in `notes`
                  ; (swap! measures update-in [measure-index beat-index] notes)
                  (update-measures measure-index beat-index compiled-notes) ; TODO: ensure notes contain duration
                  (println "!!! new measures (post update)" @measures)
                  (swap! beat-cursor + beats)
                  (println "!!! new beat cursor" @beat-cursor)))}
        reduced-track));)

    ; then transform the :pairs into slices based on the lowest common beat
    ; NOTE: may also want to consider the tempo here, will help minimize the efforts
    ; of the high-level player
    ; Example: 4/4 timesig, lowest 1 (whole note) = 1 element per measure
    ; Example: 4/4 timesig, lowest 1/2 (half note) = 2 elements per measure
    ; Example: 4/4 timesig, lowest 1/4 (quarter note) = 4 elements per measure
    ; Example: 3/4 timesig, lowest 1/2 (half note) = 1.5 elements per measure (?)
    ; Example: 3/4 timesig, lowest 1/4 (quarter note) = 3 elements per measure
    @measures))

; FIXME: remove this or just rename `denormalize-beats` to `denormalize-measures`, no point in having both (at least as far as I can tell right now)
(defn denormalize-measures
  ; given a slice size (number of measures per slice), returns a periodic sliced list of equaly sized measures that
  ; can be stepped through sequentially (adds a sense of 1st measure, 2nd measure, etc.)
  [track slice-size])

(defn denormalize
  ; processes an AST and returns a denormalized version of it that contains all the information necessary to interpet a track in a single stream of data (no references, all resolved values).
  ; normalize-variables
  ; normalize-beats
  [track])
