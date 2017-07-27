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

(declare get-beat-unit dereference-variables reduce-track) ; TODO: mention every method here so they are hoisted and declaration order becomes irrelevant

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
      ; NOTE: might need to "evaluate" duration (e.g. if it's like `1+1/2`)
      {:pair (fn [duration _]
               (if (< duration @lowest-duration)
                 (do (println "^^^^ setting lowest beat" duration)
                  (println "^^^^ lowest duration" @lowest-duration)
                  (reset! lowest-duration duration))))}
      reduced-track)
    (println "^^^^^ about to return lowest-duration" @lowest-duration)
    (min 1 @lowest-duration)))

(defn get-normalized-lowest-beat
  [track]
  (let [lowest-beat (get-lowest-beat track)
        beat-unit (get-beat-unit track)]
    (* lowest-beat beat-unit)))

(defn get-beats-per-measure
  [track]
  (first (get-time-signature track))) ; AKA numerator

; FIXME: this needs to consider the time signature, I think
(defn get-normalized-beats-per-measure
  [track]
  (let [lowest-beat (get-lowest-beat track)]
    (if (< lowest-beat 1) (denominator lowest-beat) lowest-beat)))

(defn get-beat-unit
  [track]
  (/ 1 (last (get-time-signature track)))) ; AKA 1/denominator

; NOTE: this can also be interpreted as "total measures" because the beats aren't normalized
; to the lowest common beat found in the track
(defn get-total-beats
  [track]
  (let [total-beats (atom 0)
        reduced-track (reduce-values track)]
    (insta/transform
      {:pair (fn [duration _]
               (swap! total-beats + duration))}
      reduced-track)
    @total-beats))

(defn get-scaled-total-beats
  ; returns the total beats based on the current time signature
  [track]
  (let [total-beats (get-total-beats track)
        beat-unit (get-beat-unit track)]
    (/ total-beats beat-unit)))

(defn get-normalized-total-beats
  [track]
  (let [total-beats (get-total-beats track)
        lowest-beat (get-normalized-lowest-beat track)]
    (/ total-beats lowest-beat)))

(defn get-total-measures
  [track]
  (get-total-beats track)) ; NOTE: beats and measures are the same w/o lowest-common-beat normalization

(defn get-total-measures-ceiled
  [track]
  (Math/ceil (get-total-beats track)))

(defn get-total-duration
  [track unit]
  (let [total-beats (get-scaled-total-beats track) ; using scaled because it's adjusted based on time signature, which is important for comparing against tempo
        tempo-bpm (get-tempo track)
        duration-minutes (/ total-beats tempo-bpm)
        duration-seconds (* duration-minutes 60)
        duration-milliseconds (* duration-seconds 1000)]
    (case unit
      :milliseconds duration-milliseconds
      :seconds duration-seconds
      :minutes duration-minutes)))

(defn get-ms-per-beat
  [track]
  (let [beats-per-measure (get-normalized-beats-per-measure track)
        total-measures (get-total-beats track)
        total-duration-ms (get-total-duration track :milliseconds)
        ms-per-measure (/ total-duration-ms total-measures)
        ms-per-beat (/ ms-per-measure beats-per-measure)]
    (float ms-per-beat)))

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

(defn normalize-measures
  [track]
  (let [total-measures (get-total-measures-ceiled track) ;(Math/ceil (get-total-measures track))
        ; total-beats (get-normalized-total-beats track) ;(get-total-beats track)
        beat-cursor (atom 0) ; NOTE: measured in whole notes, not the lowest beat! (makes parsing easier)
        ; lowest-beat (get-lowest-beat track)
        beats-per-measure (get-normalized-beats-per-measure track) ;(get-beats-per-measure track)
        ; beat-type (/ 1 lowest-beat) ; greatest is a whole note
        ; time-sig (get-time-signature)
        ; beat-unit (get-beat-unit track)
        ; measures (atom (vec (make-array Void/TYPE total-measures beats-per-measure)))
        ; measures (atom (vec (make-array clojure.lang.PersistentArrayMap total-measures beats-per-measure)))
        measures (atom (mapv #(into [] %) (make-array clojure.lang.PersistentArrayMap total-measures beats-per-measure)))
        reduced-track (reduce-track track)]
    (println "\n\nSTARTING MEASURES" track @measures)
    (println "---- total measures" total-measures)
    ; (println "---- total beats" total-beats)
    (println "---- beats-per-measure" beats-per-measure)
    (letfn [(update-measures [measure-index beat-index notes]
              (println "\tupdating measures! (mi, bi, notes)" measure-index beat-index notes)
              (println "\tcurrent measures (about to update):" @measures)
              (swap! measures assoc-in [measure-index beat-index] notes))
            ; FIXME: needs to be based on normalized beats
            (beat-indices [beat]
              (println "\tbeat-indices [beat]" beat)
              (let [;lowest-beat (get-normalized-lowest-beat track) ; SORT OF WORKS (but not really)
                    lowest-beat (get-lowest-beat track)
                    normalized-cursor (/ @beat-cursor lowest-beat) ; TODO: next up, integrate this
                    global-beat-index @beat-cursor ;(+ @beat-cursor beat)
                    ; local-beat-index (mod global-beat-index beats-per-measure)
                    local-beat-index (mod (* global-beat-index beats-per-measure) beats-per-measure)
                    measure-index (int (Math/floor (float (/ global-beat-index beats-per-measure))))]
                    ; measure-index (Math/ceil (/ (+ beat-cursor beat) measures))]
                (println "\t\t[bi] normalized lowest-beat" lowest-beat)
                (println "\t\t[bi] lowest-beat" (get-lowest-beat track))
                (println "\t\t[bi] beats-per-measure" beats-per-measure)
                (println "\t\t[bi] normalized-cursor" normalized-cursor)
                (println "\t\t[bi] global-beat-index" global-beat-index)
                (println "\t\t[bi] local-beat-index" local-beat-index)
                (println "\t\t[bi] measure-index" measure-index)
                {:measure measure-index :beat local-beat-index}))]
      (insta/transform
        {:pair (fn [beats notes]
                 (println "~~~ denorm-beats beats" beats)
                 (println "~~~ denorm-beats notes" notes)
                 (let [indices (beat-indices beats)
                       measure-index (:measure indices)
                       beat-index (:beat indices)
                       compiled-notes {:duration beats :notes notes}] ; TODO; consider adding: :indices [measure-index beat-index]
                  (println "~~~ current measures" @measures)
                  (println "~~~ compiled notes" compiled-notes)
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
; (defn denormalize-measures
;   ; given a slice size (number of measures per slice), returns a periodic sliced list of equaly sized measures that
;   ; can be stepped through sequentially (adds a sense of 1st measure, 2nd measure, etc.)
;   [track slice-size])

(defn denormalize
  ; processes an AST and returns a denormalized version of it that contains all the information necessary to interpet a track in a single stream of data (no references, all resolved values).
  ; normalize-variables
  ; normalize-beats
  [track])
