(ns bach.track
  (:require [instaparse.core :as insta]
            [bach.ast :refer [parse]]
            [bach.data :refer [hiccup-to-vector
                               hiccup-to-hash-map
                               ratio-to-vector
                               trim-matrix-row
                               inverse-ratio
                               safe-ratio]]))

(defstruct playable-track :headers :data)

(def default-tempo 120)
(def default-meter [4 4])
(def default-beat-unit (/ 1 (last default-meter)))
(def default-pulse-beat default-beat-unit)
(def default-headers {:tempo default-tempo
                      :meter default-meter
                      :beat-unit default-beat-unit
                      :pulse-beat default-pulse-beat
                      :total-beats 0
                      :total-beat-units 0
                      :total-pulse-beats 0
                      :ms-per-pulse-beat 0
                      :ms-per-beat-unit 0})

(def powers-of-two (iterate (partial * 2) 1))

(defn variable-scope
  "Provides a localized scope/stack for tracking variables"
  [scope]
  (let [context (atom {})]
    (letfn [(variables []
              (get @context :vars {}))
            ; TODO: might just want to move this into `dereference-variables`
            (create-variable [label value]
              (swap! context assoc :vars (conj (variables) [label value])))]
      (scope variables create-variable context))))

; TODO: integreate reduce-values
; TODO: check for :play
; TODO: validate any variables in :play
(defn validate
  "Determines if a parsed track is valid or not"
  [track]
  (variable-scope
   (fn [variables create-variable _]
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
                (when (not (some #{bottom} (take 10 powers-of-two)))
                  (throw (Exception. "note divisors must be base 2 and no greater than 512")))))
       :tempo (fn [& tempo-token]
                (let [tempo (-> tempo-token last read-string)]
                  (when (not (<= 0 tempo 256))
                    (throw (Exception. "tempos must be between 0 and 256 beats per minute")))))}
      track)))
  true)

(def validate-memo (memoize validate))

(defn deref-variables
  "Dereferences any variables found in the parsed track. Does NOT support hoisting (yet)"
  [track]
  (variable-scope
   (fn [variables track-variable _]
     (insta/transform
      {:assign (fn [label-token value-token]
                 (let [label (last label-token)
                       value (last value-token)
                       [value-type] value-token]
                   (case value-type
                     :identifier
                     (let [stack-value (get (variables) value)]
                       (track-variable label stack-value)
                       [:assign label-token stack-value])
                     (do (track-variable label value-token)
                         [:assign label-token value-token]))))
       :identifier (fn [label]
                     (let [stack-value (get (variables) label)]
                       (if stack-value stack-value [:identifier label])))
       :play (fn [value-token]
               (let [[& value] value-token
                     [value-type] value-token]
                 (case value-type
                   :identifier
                   (let [stack-value (get (variables) value)]
                     [:play stack-value])
                   [:play value-token])))}
      track))))

(defn reduce-values
  "Reduces any primitive values in a parsed track"
  [track]
  (insta/transform
   {:add +,
    :sub -,
    :mul *,
    :div /,
    :meter (fn [n d] [n d]),
    :number clojure.edn/read-string,
    ; TODO: Determine if this is necessary with our math grammar (recommended in instaparse docs)
    ; :expr identity,
    :string #(clojure.string/replace % #"^(\"|\')|(\"|\')$" "")} track))

(defn reduce-track
  "Dereferences variables and reduces the primitive values in a parsed track"
  [track]
  (-> track
      deref-variables
      reduce-values))

(defn normalize-duration
  "Adjusts a beat's duration from being based on whole notes (i.e. 1 = 4 quarter notes) to being based on the provided beat unit (i.e. the duration of a single normalized beat, in whole notes).
  In general, this determines 'How many `unit`s` does the provided `duration` in this `meter` (i.e. time-sig)?'."
  [duration unit meter]
  (let [inverse-meter (inverse-ratio (rationalize meter))
        inverse-unit (inverse-ratio (rationalize unit))
        within-measure? (<= duration meter)]
    (if within-measure?
      (/ duration unit)
      (* duration (max inverse-unit inverse-meter)))))

(defn get-headers
  "Provides the headers (aka meta info) for a parsed track"
  [track]
  (let [headers (atom default-headers)
        reduced-track (reduce-track track)] ; TODO: might not want this at this level, should probably be called higher up
    (insta/transform
     {:header (fn [kind-token value]
                (let [kind (last kind-token)
                      header-key (keyword (clojure.string/lower-case kind))]
                  (swap! headers assoc header-key value)))}
     reduced-track)
    @headers))

(defn find-header
  "Generically finds a header entry / meta tag in a parsed track by its label"
  [track label default]
  (let [header (atom default)]
    (insta/transform
     {:header (fn [meta-key value]
                (let [kind (last meta-key)]
                  (when (= kind label)
                    (reset! header value))))}
     track)
    @header))

(defn get-meter
  "Determines the global meter, or time signature, of the track. Localized meters are NOT supported yet."
  [track]
  (let [reduced-track (reduce-values track)
        header (find-header reduced-track "Meter" default-meter)]
    header))

(defn get-meter-ratio
  "Determines the global meter ratio of the track.
   For example: The ratio of the 6|8 meter is 3/4."
  [track]
  (let [[beats-per-measure & [beat-unit]] (get-meter track)]
    (/ beats-per-measure beat-unit)))

(defn get-tempo
  "Determines the global tempo of the track. Localized tempos are NOT supported yet."
  [track]
  (find-header track "Tempo" default-tempo))

(defn get-beat-unit
  "Determines the reference unit to use for beats, based on time signature"
  [track]
  (/ 1 (last (get-meter track)))) ; AKA 1/denominator

(defn get-beat-unit-ratio
  "Determines the ratio between the beat unit and the number of beats per measure"
  [track]
  (let [[beats-per-measure & [beat-unit]] (get-meter track)]
    (mod beat-unit beats-per-measure)))

(defn get-beats-per-measure
  "Determines how many beats are in each measure, based on the time signature"
  [track]
  (first (get-meter track))) ; AKA numerator

(defn get-pulse-beat
  "Determines the greatest common beat (by duration) among every beat in a track.
   Once this beat is found, a track can be iterated through evenly (and without variance) via an arbitrary interval, timer, etc.
   This differs from the frame spotting approach which queries the current measure/beat on each frame tick."
  [track]
  ; FIXME: Use ##Inf instead in `lowest-duration` once we upgrade to Clojure 1.9.946+
  ; @see: https://cljs.github.io/api/syntax/Inf
  (let [lowest-duration (atom 1024)
        reduced-track (reduce-values track)]
    (insta/transform
      ; NOTE: might need to "evaluate" duration (e.g. if it's like `1+1/2`)
     {:pair (fn [duration _]
              (when (< duration @lowest-duration)
                (reset! lowest-duration duration)))}
     reduced-track)
    (let [beat-unit (get-beat-unit reduced-track)
          meter (get-meter-ratio reduced-track)
          full-measure meter
          pulse-beat @lowest-duration
          pulse-beat-unit (/ 1 (-> pulse-beat
                                   rationalize
                                   clojure.lang.Numbers/toRatio
                                   denominator))
          pulse-beat-aligns? (= 0 (mod (max pulse-beat meter)
                                       (min pulse-beat meter)))]
      (if pulse-beat-aligns?
        (min pulse-beat full-measure)
        (min pulse-beat-unit beat-unit)))))

(defn get-normalized-beats-per-measure
  "Determines how many beats are in a measure, normalized against the pulse beat of the track"
  [track]
  (let [pulse-beat (get-pulse-beat track)
        meter (get-meter-ratio track)]
    (safe-ratio
      (max pulse-beat meter)
      (min pulse-beat meter))))

(defn get-total-beats
  "Determines the total number of beats in the track.
   Beats are represented in traditional semibreves/whole notes and crotchet/quarternotes.
   In other words, a beat with a duration of 1 is equivalant to 4 quarter notes, or 1 measure in 4|4 time."
  [track]
  (let [total-beats (atom 0)
        reduced-track (reduce-values track)]
    (insta/transform
     {:pair (fn [duration _]
              (swap! total-beats + duration))}
     reduced-track)
    @total-beats))

(defn get-scaled-total-beats
  "Determines the total number of beats in the track scaled to the beat unit (4/4 time, 4 beats = four quarter notes)"
  [track]
  (safe-ratio
    (get-total-beats track)
    (get-beat-unit track)))

(defn get-normalized-total-beats
  "Determines the total beats in a track normalized to the pulse beat of the track"
  [track]
  (let [total-beats (get-total-beats track)
        pulse-beat (get-pulse-beat track)]
    (safe-ratio
      (max total-beats pulse-beat)
      (min total-beats pulse-beat))))

; TODO: Just remove this, only beneficial in 4|4 time. Pointless elswhere since already handled by get-normalized-total-measures and get-normalized-total-beats.
(defn get-total-measures
  "Determines the total number of measures in the track.
   Beats and measures are equivelant here since the beats are normalized to traditional semibreves/whole notes and crotchet/quarternotes.
  In other words, a beat with a duration of 1 is equivalant to 4 quarter notes, or 1 measure in 4|4 time."
  [track]
  (get-total-beats track))

; TODO: Consider renaming to `get-total-bars`
(defn get-normalized-total-measures
  "Determines the total number of measures in a track, normalized to the pulse beat."
  [track]
  (let [total-beats (get-normalized-total-beats track)
        beats-per-measure (get-normalized-beats-per-measure track)]
    (safe-ratio total-beats beats-per-measure)))

(defn get-total-duration
  "Determines the total time duration of a track (milliseconds, seconds, minutes)."
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

; TODO: Write tests
(defn get-scaled-ms-per-beat
  "Determines the number of milliseconds each beat should be played for (scaled to the beat unit)."
  [track]
  (let [reduced-track (reduce-track track)
        tempo (get-tempo reduced-track)
        beat-unit (get-beat-unit reduced-track)
        beats-per-second (/ tempo 60)
        seconds-per-beat (/ 1 beats-per-second)
        ms-per-beat (* seconds-per-beat 1000)]
    (float ms-per-beat)))

(defn get-normalized-ms-per-beat
  "Determines the number of milliseconds each beat should be played for (normalized to the pulse beat).
   Primarily exists to make parsing simple and optimized in the high-level interpreter / player.
   Referred to as 'normalized' because, as of now, all compiled beat durations (via `compile-track`) are normalized to the pulse beat.
   References:
     http://moz.ac.at/sem/lehre/lib/cdp/cdpr5/html/timechart.htm
     https://music.stackexchange.com/a/24141"
  [track]
  (let [reduced-track (reduce-track track)
        ms-per-beat-unit (get-scaled-ms-per-beat reduced-track)
        beat-unit (get-beat-unit reduced-track)
        pulse-beat (get-pulse-beat reduced-track)
        pulse-to-unit-beat-ratio (/ pulse-beat beat-unit)
        ms-per-pulse-beat (* ms-per-beat-unit pulse-to-unit-beat-ratio)]
    (float ms-per-pulse-beat)))

(defn get-ms-per-beat
  "Dynamically determines the ms-per-beat based on the kind of the beat, either :pulse (default) or :unit."
  ([track kind]
   (case kind
     :pulse (get-normalized-ms-per-beat track)
     :unit (get-scaled-ms-per-beat track)))
  ([track]
   (get-normalized-ms-per-beat track)))

(defn normalize-measures
  "Parses the track data exported via `Play` into a normalized matrix where each row (measure) has the same number of elements (beats).
   Makes parsing the track much easier for the high-level interpreter / player as the matrix is trivial to iterate through."
  [track]
  (let [beat-cursor (atom 0)
        meter (get-meter-ratio track)
        pulse-beat (get-pulse-beat track)
        beats-per-measure (get-normalized-beats-per-measure track)
        total-measures (Math/ceil (get-normalized-total-measures track))
        total-beats (get-normalized-total-beats track)
        unused-tail-beats (mod (max total-beats beats-per-measure) (min total-beats beats-per-measure))
        measure-matrix (mapv #(into [] %) (make-array clojure.lang.PersistentArrayMap total-measures beats-per-measure))
        measures (atom (trim-matrix-row measure-matrix (- (count measure-matrix) 1) unused-tail-beats))
        reduced-track (reduce-track track)]
    (insta/transform
      ; We only want to reduce the notes exported via the `Play` construct, otherwise it's ambiguous what to use
     {:play (fn [play-track]
              (letfn [(cast-duration [duration]
                        (int (normalize-duration duration pulse-beat meter)))
                      (compile-notes [notes]
                        (->> [notes] hiccup-to-hash-map flatten (map :atom) vec))
                      (update-cursor [beats]
                        (swap! beat-cursor + beats))
                      (update-measures [measure-index beat-index notes]
                        (swap! measures assoc-in [measure-index beat-index] notes))
                      (beat-indices [beat]
                        (let [global-beat-index @beat-cursor
                              local-beat-index (mod global-beat-index beats-per-measure)
                              measure-index (int (Math/floor (/ global-beat-index beats-per-measure)))]
                          {:measure measure-index :beat local-beat-index}))]
                (insta/transform
                ; TODO: Generally rename `notes` to `items`. Makes more sense since a beat can contain more than just notes.
                 {:pair (fn [duration notes]
                          (let [beats (cast-duration duration)
                                indices (beat-indices beats)
                                measure-index (:measure indices)
                                beat-index (:beat indices)
                                compiled-notes {:duration beats ; i.e. pulses
                                                :notes (compile-notes notes)}]
                            (update-measures measure-index beat-index compiled-notes)
                            (update-cursor beats)))}
                 play-track)))}
     reduced-track)
    @measures))

(defn provision-headers
  "Combines default static meta information with dynamic meta information to provide a provisioned set of headers.
  Several headers could easily be calculated by a client interpreter, but they are intentionally defined here to refine rhythmic semantics and simplify synchronization."
  [track]
  (let [headers (get-headers track)
        meter (get-meter track)
        total-beats (get-total-beats track)
        total-beat-units (get-scaled-total-beats track)
        total-pulse-beats (get-normalized-total-beats track)
        ms-per-pulse-beat (get-ms-per-beat track :pulse)
        ms-per-beat-unit (get-ms-per-beat track :unit)
        beat-unit (get-beat-unit track)
        pulse-beat (get-pulse-beat track)]
    (assoc headers
           :meter meter
           :total-beats total-beats
           :total-beat-units total-beat-units
           :total-pulse-beats total-pulse-beats
           :ms-per-pulse-beat ms-per-pulse-beat
           :ms-per-beat-unit ms-per-beat-unit
           :beat-unit beat-unit
           :pulse-beat pulse-beat)))

; TODO: Allow track to be provisioned in flat/stream mode (i.e. no measures, just evenly sized beats)
(defn provision
  "Provisions a parsed track, generating and organizating all of the information necessary to easily
   interpret a track as a single stream of normalized data (no references, all values are resolved and optimized)."
  [track]
  (when (validate track)
    (let [headers (provision-headers track)
          data (normalize-measures track)]
          ; TODO: version
      (struct playable-track headers data))))

(defn compose
  "Creates a normalized playable track from either a parsed AST or a UTF-8 string of bach data.
   A 'playable' track is formatted so that it is easily iterated over by a high-level Bach engine."
  [track]
  (cond
    (vector? track)
      (provision track)
    (string? track)
      (-> track parse provision)
    :else
      (throw (Exception. (str "Cannot compose track, provided unsupported data format. Must be a parsed AST vector or a UTF-8 encoded string.")))))
