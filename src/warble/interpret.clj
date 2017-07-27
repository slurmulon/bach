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
                 (reset! time-signature value)))} ; TODO: need to ensure this ends up as a 2-element list instead of a ratio [num, denom]
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
                 (reset! lowest-duration duration)))}
      reduced-track)
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
  (let [beat-cursor (atom 0) ; NOTE: measured in time-scaled/whole notes, not the lowest beat! (makes parsing easier)
        beats-per-measure (get-normalized-beats-per-measure track)
        total-measures (get-total-measures-ceiled track)
        measures (atom (mapv #(into [] %) (make-array clojure.lang.PersistentArrayMap total-measures beats-per-measure)))
        reduced-track (reduce-track track)]
    (letfn [(update-measures [measure-index beat-index notes]
              (swap! measures assoc-in [measure-index beat-index] notes))
            (beat-indices [beat]
              (let [lowest-beat (get-lowest-beat track)
                    normalized-beat-cursor (/ @beat-cursor lowest-beat)
                    global-beat-index normalized-beat-cursor
                    local-beat-index (mod global-beat-index beats-per-measure)
                    measure-index (int (Math/floor (/ global-beat-index beats-per-measure)))]
                {:measure measure-index :beat local-beat-index}))]
      (insta/transform
        {:pair (fn [beats notes]
                 (let [indices (beat-indices beats)
                       measure-index (:measure indices)
                       beat-index (:beat indices)
                       compiled-notes {:duration beats :notes notes}] ; TODO; consider adding: :indices [measure-index beat-index]
                  (update-measures measure-index beat-index compiled-notes)
                  (swap! beat-cursor + beats)))}
        reduced-track))
    @measures))

(defn denormalize
  ; processes an AST and returns a denormalized version of it that contains all the information necessary to interpet a track in a single stream of data (no references, all resolved values).
  ; normalize-variables
  ; normalize-beats
  [track])
