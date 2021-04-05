; # Help
; https://gist.github.com/stathissideris/1397681b9c63f09c6992
; https://rmulhol.github.io/clojure/2015/05/12/flatten-tree-seq.html
; http://gigasquidsoftware.com/blog/2013/05/01/growing-a-language-with-clojure-and-instaparse/
; https://stackoverflow.com/questions/60591121/how-can-i-turn-an-ordered-tree-into-a-collection-of-named-nodes-in-clojure
; https://dnaeon.github.io/graphs-and-clojure/

; # History
; https://github.com/slurmulon/bach/blob/c04c808193f1f278c2779111b1f029cedd4af4b1/src/bach/track.clj

(ns bach.compose
  (:require [instaparse.core :as insta]
            [bach.ast :refer [parse]]
            [bach.data :refer [hiccup-to-hash-map
                               ratio-to-vector
                               trim-matrix-row
                               inverse-ratio
                               safe-ratio
                               to-json
                               to-string
                               math-floor
                               math-ceil
                               powers-of-two
                               gcd
                               itemize
                               linearize
                               linearize-indices
                               stretch
                               quantize
                               cast-tree
                               post-tree
                               flatten-by
                               flatten-one
                               greatest-in
                               transpose
                               problem]]))

(def default-tempo 120)
(def default-meter [4 4])
(def default-beat-unit (/ 1 (last default-meter)))
(def default-pulse-beat default-beat-unit)
(def default-headers {:tempo default-tempo
                      :meter default-meter
                      :beat-unit default-beat-unit
                      :pulse-beat default-pulse-beat
                      :beat-units-per-measure 4
                      :pulse-beats-per-measure 4
                      :total-beats 0
                      :total-beat-units 0
                      :total-pulse-beats 0
                      :ms-per-pulse-beat 0
                      :ms-per-beat-unit 0})

(defn variable-scope
  "Provides a localized scope/stack for tracking variables."
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
  "Determines if a parsed track is valid or not."
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
                     (when (-> (variables) (contains? value) not)
                       (problem (str "Variable is not declared before it's used: " value ", " (variables))))
                     (create-variable label value))))
       :div (fn [top-token bottom-token]
              (let [top    (-> top-token    last to-string)
                    bottom (-> bottom-token last to-string)]
                (when (not (some #{bottom} (take 10 powers-of-two)))
                  (problem "Note divisors must be even and no greater than 512"))))
       :tempo (fn [& tempo-token]
                (let [tempo (-> tempo-token last to-string)]
                  (when (not (<= 0 tempo 256))
                    (problem "Tempos must be between 0 and 256 beats per minute"))))}
      track)))
  true)

(def validate-memo (memoize validate))

(defn deref-variables
  "Dereferences any variables found in the parsed track. Does NOT support hoisting (yet)."
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
  "Reduces any primitive values in a parsed track."
  [track]
  (insta/transform
   {:add +,
    :sub -,
    :mul *,
    :div /,
    :meter (fn [n d] [n d]),
    :number to-string,
    :name to-string,
    ; TODO: Determine if this is necessary with our math grammar (recommended in instaparse docs)
    ; :expr identity,
    :string #(clojure.string/replace % #"^(\"|\')|(\"|\')$" "")} track))

(defn reduce-track
  "Dereferences variables and reduces the primitive values in a parsed track."
  [track]
  (-> track
      deref-variables
      reduce-values))

(defn normalize-duration
  "Adjusts a beat's duration from being based on whole notes (i.e. 1 = 4 quarter notes) to being based on the provided beat unit (i.e. the duration of a single normalized beat, in whole notes).
  In general, this determines 'How many `unit`s` does the provided `duration` equate to in this `meter`?'."
  [duration unit meter]
  (let [inverse-unit (inverse-ratio #?(:clj (rationalize unit) :cljs unit))
        inverse-meter (inverse-ratio #?(:clj (rationalize meter) :cljs meter))
        within-measure? (<= duration meter)]
    (if within-measure?
      (/ duration unit)
      (* duration (max inverse-unit inverse-meter)))))

(defn get-headers
  "Provides the headers (aka meta info) for a parsed track"
  [track]
  (let [headers (atom default-headers)
        reduced-track (reduce-track track)]
    (insta/transform
     {:header (fn [kind-token value]
                (let [kind (last kind-token)
                      header-key (-> kind name clojure.string/lower-case keyword)]
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
                  (when (= (str kind) (str label))
                    (reset! header value))))}
     track)
    @header))

(defn get-tempo
  "Determines the global tempo of the track. Localized tempos are NOT supported yet."
  [track]
  (find-header track "Tempo" default-tempo))

(defn get-meter
  "Determines the global meter, or time signature, of the track. Localized meters are NOT supported yet."
  [track]
  (let [reduced-track (reduce-values track)]
    (find-header reduced-track "Meter" default-meter)))

(defn get-meter-ratio
  "Determines the global meter ratio of the track.
   For example: The ratio of the 6|8 meter is 3/4."
  [track]
  (let [[beats-per-measure & [beat-unit]] (get-meter track)]
    (/ beats-per-measure beat-unit)))

(defn get-beat-unit
  "Determines the reference unit to use for beats, based on time signature."
  [track]
  (/ 1 (last (get-meter track)))) ; AKA 1/denominator

(defn get-beat-unit-ratio
  "Determines the ratio between the beat unit and the number of beats per measure."
  [track]
  (let [[beats-per-measure & [beat-unit]] (get-meter track)]
    (mod beat-unit beats-per-measure)))

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
          pulse-beat-unit (gcd pulse-beat meter)
          pulse-beat-aligns? (= 0 (mod (max pulse-beat meter)
                                       (min pulse-beat meter)))]
      (if pulse-beat-aligns?
        (min pulse-beat full-measure)
        (min pulse-beat-unit beat-unit)))))

(defn get-beats-per-measure
  "Determines how many beats are in each measure, based on the time signature."
  [track]
  (first (get-meter track))) ; AKA numerator

(def get-scaled-beats-per-measure get-beats-per-measure)
(def get-beat-units-per-measure get-beats-per-measure)

(defn get-normalized-beats-per-measure
  "Determines how many beats are in a measure, normalized against the pulse beat of the track."
  [track]
  (let [pulse-beat (get-pulse-beat track)
        meter (get-meter-ratio track)]
    (safe-ratio
     (max pulse-beat meter)
     (min pulse-beat meter))))

(def get-pulse-beats-per-measure get-normalized-beats-per-measure)

(defn get-total-beats
  "Determines the total number of beats in the track.
   Beats are represented in traditional semibreves/whole notes and crotchets/quarternotes.
   In other words, a beat with a duration of 1 is strictly equivalent to 4 quarter notes, or 1 measure in 4|4 time."
  [track]
  (let [total-beats (atom 0)
        reduced-track (reduce-values track)]
    (insta/transform
     {:pair (fn [duration _]
              (swap! total-beats + duration))}
     reduced-track)
    @total-beats))

(defn get-scaled-total-beats
  "Determines the total number of beats in the track scaled to the beat unit (4/4 time, 4 beats = four quarter notes)."
  [track]
  (safe-ratio
   (get-total-beats track)
   (get-beat-unit track)))

(def get-total-beat-units get-scaled-total-beats)

(defn get-normalized-total-beats
  "Determines the total beats in a track normalized to the pulse beat of the track."
  [track]
  (let [total-beats (get-total-beats track)
        pulse-beat (get-pulse-beat track)]
    (safe-ratio
     (max total-beats pulse-beat)
     (min total-beats pulse-beat))))

(def get-total-pulse-beats get-normalized-total-beats)

; TODO: Consider removing. Useful for consistency and predictability but otherwise redundant.
(defn get-total-measures
  "Determines the total number of measures defined in the track.
   Beats and measures are equivelant here since the beats are normalized to traditional semibreves/whole notes and crotchet/quarternotes.
  In other words, a beat with a duration of 1 is strictly equivalant to 4 quarter notes, or 1 measure in 4|4 time."
  [track]
  (get-total-beats track))

(defn get-scaled-total-measures
  "Determines the total number of measures in a track scaled to the beat unit (e.g. 6|8 time, 12 eigth notes = 2 measures)."
  [track]
  (safe-ratio
   (get-scaled-total-beats track)
   (get-beats-per-measure track)))

(defn get-normalized-total-measures
  "Determines the total number of measures in a track, normalized to the pulse beat."
  [track]
  (safe-ratio
   (get-normalized-total-beats track)
   (get-normalized-beats-per-measure track)))

(defn get-total-duration
  "Determines the total time duration of a track (milliseconds, seconds, minutes).
   Uses scaled total beats (i.e. normalized to the track's beat unit) to properly adjust
   the value based on the time signature, important for comparing against BPM in all meters."
  [track unit]
  ; Using scaled total beats because it's adjusted based on time signature, which is important for comparing against tempo
  (let [total-beats (get-scaled-total-beats track)
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

(def get-ms-per-beat-unit get-scaled-ms-per-beat)

(defn get-normalized-ms-per-beat
  "Determines the number of milliseconds each beat should be played for (normalized to the pulse beat).
   Primarily exists to make parsing simple and optimized in the high-level interpreter / player.
   Referred to as 'normalized' because, as of now, all beat durations (via `compose`) are normalized to the pulse beat.
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

(def get-ms-per-pulse-beat get-normalized-ms-per-beat)

(defn get-ms-per-beat
  "Dynamically determines the ms-per-beat based on the kind of the beat, either :pulse (default) or :unit."
  ([track kind]
   (case kind
     :pulse (get-normalized-ms-per-beat track)
     :unit (get-scaled-ms-per-beat track)))
  ([track]
   (get-normalized-ms-per-beat track)))

(defn normalize-measures
  "Parses the track data exported via `!Play` into a normalized matrix where each row (measure) has the same number of elements (beats).
   Makes parsing the track much easier for the high-level interpreter / player as the matrix is trivial to iterate through.
   Bach interpreters can simply iterate one measure and beat at a time for `ms-per-pulse-beat` milliseconds on each step.
   This strongly favors applications that must optimize and minimize their synchronization points."
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
                      (cast-elements [elements]
                        (->> [elements] hiccup-to-hash-map flatten (map :atom) vec))
                      (update-cursor [beats]
                        (swap! beat-cursor + beats))
                      (update-measures [measure-index beat-index elements]
                        (swap! measures assoc-in [measure-index beat-index] elements))
                      (beat-indices []
                        (let [global-beat-index @beat-cursor
                              local-beat-index (mod global-beat-index beats-per-measure)
                              measure-index (int (math-floor (/ global-beat-index beats-per-measure)))]
                          {:measure measure-index :beat local-beat-index}))]
                (insta/transform
                 {:pair (fn [duration elements]
                          (let [indices (beat-indices)
                                measure-index (:measure indices)
                                beat-index (:beat indices)
                                normalized-items {:duration (cast-duration duration)
                                                  :items (cast-elements elements)}]
                            (update-measures measure-index beat-index normalized-items)
                            (update-cursor (:duration normalized-items))))}
                 play-track)))}
     reduced-track)
    @measures))



  ; defn normalize-elements
  ;  - Transforms native AST tree structure into a simplified form, preparing it for consumption

  ; Iterate through each high-level loop exported by Play!
  ;  - May need to post-process instaparse/transform
  ; For each of these root loops, reduce all nested loops and their elements, in order. "Reduce" meaning:
  ;  - Combined total duration (in pulse beats!) of child loops
  ;    - Use `normalize-duration` for this
  ;  - Upsert list of elements that occur at each pulse beat
  ; NOTE:
  ;  - `quantize-track`, once written, should use the output of this to create a step-wise list where each element represents a single pulse beat
; (defn linearize-track
;   [track]
;   (let [meter (get-meter-ratio track)
;         pulse-beat (get-pulse-beat track)]
;     (insta/transform
;       {:play normalize-collections}
;       track)))

(defn as-durations
  [tree]
  (cast-tree map? :duration tree))

; TODO: Detect cyclic references!
;  - Should do this more generically in `reduce-values` or the like, instead of here
; TODO: Rename to normalize-collections or reduce-collections
(defn normalize-collections
  "Normalizes all collections in parsed AST tree as native clojure structures, for easier handling (mostly around reduction) in subsequent functions.
   Input: [:list :a [:set :b :c] [:set :d [:list :e :f]]]
   Ouput: [:a #{:b :c} #{:d [:e :f]}]"
  [tree]
  (->> tree
    reduce-values
    ; reduce-track
    ; NOTE: On the right path, but we don't want to set :elements this early (will mess up following stuff)
    ;  - After this, THEN we will pipe into the `:pair` transformer, and THEN we can group, reduce and linearize
    ; NOTE/TODO: Probably just move this all to `reduce-values`, if possible
    (insta/transform
      {:list (fn [& [:as all]] (vec all))
      ; {:list (fn [& [:as all]] (flatten (vec all)))
       :set (fn [& [:as all]] (into #{} all))
       :loop (fn [iters & [:as all]] (->> all (mapcat #(itemize iters %)) flatten))
       :pair #(assoc {} :duration %1 :elements %2)})))


(defn reduce-durations
  [tree]
  (clojure.walk/postwalk
    #(cond
       (set? %) (flatten-by max (seq %))
       (vector? %) (flatten-by + %)
       :else %)
    tree))

(defn as-reduced-durations
  [tree]
  (->> tree as-durations reduce-durations))

; TODO: Rename to `normalize-durations`
; (defn reduce-durations
(defn normalize-durations
  [tree]
  (->> tree
    normalize-collections
    ; NOTE: These are NOT normalized durations (raw durations, not pulse beats)
    ;  - TODO: Consider normalizing durations to pulse beats earlier, such as in normalize-collections
    as-durations
    reduce-durations))

; (def linearize-collection-tree
;   [tree]
;   (let [norm-tree (normalize-collections tree)
        ; TODO (next up!): mirror `normalize-beat` and `normalize-beats` in v3 draft script!
  ; (->> tree
  ;      normalize-collections
  ;      (cast-tree set? :duration)

; (def normalize-beat-pairs
; ; (defn reduce-list-tree
;   [tree]
;   (insta/transform
;     ; WARN: Actually we dont' want to do this here. This should happen outside of this. All this method should care about is reducing loops into weighted nodes.
;     ;  - TODO: Do this AFTER
;     {:pair #(assoc {} :duration %1 :elements %2)}
;     ; {:pair (fn [duration elements]
;     ;          {:duration duration
;     ;           :elements elements})}
;     tree))

; (defn align-beats
;   [beats]
;   (let [items (normalize-collections beats)

(defn normalize-beat
  "Reduces and normalizes a beat and all of its child beats (i.e. beats occuring at the same point in time) into a single element at the provided quantized index."
  ([beat] (normalize-beat beat 0))
  ; ([beat positions]
  ([beat index]
    (let [beats (cond (map? beat) [beat] (coll? beat) beat)
          elements (flatten beats)
          ; FIXME: Don't necessarily want to use greatest-in here!
          ;  - Should just be able to use `reduce-durations`, I think
          ; duration (-> beats as-durations greatest-in)]
          duration (-> beats as-reduced-durations)]
      {:index index
       ; NOTE: elements is probably temporary as is (should eventually be replaced with "play" and "stop" props)
       :elements elements
       :duration duration})))

; TODO: Need to first linearize beats
;  - Need to map through each set in a column (see map docs), then conflate/flaten them
;    - TODO: Probably break out `linearize-beat-set`
;  - e.g.
;    Input: [:list [:set [:list [{:duration 2 :data :a} {:duration 3 :data :b}]
;                        [:list [:duration 1 :data :a2} {:duration 4 :data :b2}]]]
;                   {:duration 5 :data :c}]]
;    Input2 [#{[{:duration 2 :data :a} {:duration 3 :data :b}]
;              [{:duration 1 :data :a2} {:duration 4 :data :b2}]}
;            {:duration 5 :data :c}]
;    Output: [#{{:duration 2 :data :a} {:duration 1 :data :a2}}
;             #{{:duration 3 :data :b} {:duration 4 :data :b2}}
;             {:duration 5 :data :c}]]

; AKA (and maybe instead) linearize-beat-sets
(defn linearize-collections
  [tree]
  (->> tree
       normalize-collections
       ; FIXME: Causes some infinite recursion craziness (might need to do postwalk instead of prewalk)
       (post-tree set? (fn [set-coll]
                         (println "^^^^^ set coll" set-coll)
                         ; TODO: Might need to make this deep and provide a semi-flattened list (still needs to be 2d at least for transpose)
                         ;  - Or, we can just map all beats as single-element vectors before processing
                         ; NOTE: set-items normalizes all items in set as a vector for easier processing
                         ;   - This is the original format we played with in bach-research, which is now proving to be useful (for transpose)
                         (let [set-items (map #(cond
                                                 (sequential? %) (vec %)
                                                 ; (set? %) (vec %)
                                                 ; (vector? %) %
                                                 :else [%]) set-coll)
                         ; (let [set-items (map #(if (coll? %) % [%]) set-coll)
                               set-vecs (filter vector? set-coll)]
                           ; FIXME: Needs to also consider maps (i.e. beat pairs)
                           ;  - ALSO: We want to return a vecotr instead of a set when we have set-vecs
                           ;  - @see: https://stackoverflow.com/a/29240104
                           ; (transpose set-vecs)
                           ; (-> set-vecs transpose set))))))
                           (println "SET ITEMS" set-items)
                           (->> set-items transpose (map set)))))))
                           ; (apply (map conj #{}) set-vecs))))))
                           ; (cast-tree vector? #(map conj #{} %) set-coll)


(defn position-beats
  [beats]
  (let [;durations (as-durations beats)
        coll (normalize-collections beats)
        durations (map as-reduced-durations coll)
        ; durations (map as-durations coll)
        ; indices (linearize-indices reduce-durations durations)
        indices (linearize-indices identity durations)]
        ; durations (->> beats normalize-durations (println "---- durations"))]
        ; durations (map as-reduced-durations beats)]
        ; durations (->> beats normalize-collections as-durations (println "---- durations"))]
    (println "---- durations" durations)
    (println "---- indices" indices)
    ; WARN: Only for testing, what we really want is to return this struct for each beat
    ;   - To achieve this, beats must be linearized (grouped by order of occurance based on durations)
    ;   - In order to achieve this grouping, see comment above defn
    {:durations durations
     :indices indices}))
        ; indices (do (println "!!!!!") (linearize-indices #(map reduce-durations %) durations))]
    ; (map #(assoc :durations %1
        ;          :indicies %2) beats)))
    ; (map (fn [beat]
    ;       ; (let [durations (as-durations beat)
    ;       ;       ; FIXME linearize-indices needs to use `reduce-durations` logic, NOT greatest-in (as in `linearize`)
    ;       ;       indices ]
    ;         (assoc :durations durations
    ;                 :indices indices)))))

(defn normalize-beats
  [beats]
  ; (juxt 
  ; (let [positions (map position-beat beats)]
  ;   (map normalize-beat beats positions)))

  (map (juxt position-beats normalize-beat) beats))

  ; FIXME: Don't necessarily want to use greatest-in here!
  ; (let [indices (-> beats as-reduced-durations linearize-indices)]
  ; (let [positions (map (assoc :durations (as-reduced-durations %)
  ;                             :indices (linearize-indices )
        ; indices (->> beats (map as-reduced-durations) linearize-indices)]
    ; (map normalize-beat beats indices)))

; (defn linearize-beats
;   [beats]
;   (map (
  ; (let [xf (comp #(normalize-beat %))]
  ; (let [xf identity]
  ;   (transduce xf (comp normalize-collections conj) beats)))
  ; (transduce identity conj beats))

  ; TODO: Now (maybe) reduce sets separately, so the final structure we work with matches our original tree structure in bach-research
  ;  - Well, in our case is easier to just merge sets and retain separate set/list structs

(defn reduce-set-tree
  [tree]
  nil)

; TODO: Rename to just normalize eventually
(defn linearize-track
  [track]
  (let [meter (get-meter-ratio track)
        pulse-beat (get-pulse-beat track)]
    (insta/transform
      {:play normalize-collections}
      track)))


(defn stretch-loop-tree
  "Input (bach): 2 of [1 -> :a, 2 -> :z]
   Ouput (pseudo): [:a :z :z :a :z :z]"
  [tree]
  (insta/transform
    ; {:loop #(itemize % %)}
    {:loop (fn [iterations coll]
             (itemize iterations coll))}
     tree))

(defn signify-normalized-beats
  []
  nil)

; i.e. quantize
; defn stepify

; provision-units

(defn provision-headers
  "Combines default static meta information with dynamic meta information to provide a provisioned set of headers.
  Several headers could easily be calculated by a client interpreter, but they are intentionally defined here to refine rhythmic semantics and simplify synchronization."
  [track]
  (let [headers (get-headers track)
        meter (get-meter track)
        total-beats (get-total-beats track)
        total-beat-units (get-total-beat-units track)
        total-pulse-beats (get-total-pulse-beats track)
        beat-units-per-measure (get-beat-units-per-measure track)
        pulse-beats-per-measure (get-pulse-beats-per-measure track)
        ms-per-beat-unit (get-ms-per-beat track :unit)
        ms-per-pulse-beat (get-ms-per-beat track :pulse)
        beat-unit (get-beat-unit track)
        pulse-beat (get-pulse-beat track)]
    (assoc headers
           :meter meter
           :total-beats total-beats
           :total-beat-units total-beat-units
           :total-pulse-beats total-pulse-beats
           :beat-units-per-measure beat-units-per-measure
           :pulse-beats-per-measure pulse-beats-per-measure
           :ms-per-beat-unit ms-per-beat-unit
           :ms-per-pulse-beat ms-per-pulse-beat
           :beat-unit beat-unit
           :pulse-beat pulse-beat)))

; TODO: Allow track to be provisioned in flat/stream mode (i.e. no measures, just evenly sized beats)
; TODO: Add `version` property
(defn provision
  "Provisions a parsed track, generating and organizating all of the information necessary to easily
   interpret a track as a single stream of normalized data (no references, all values are resolved and optimized)."
  [track]
  (when (validate track)
    (let [headers (provision-headers track)
          data (normalize-measures track)
          source {:headers headers :data data}]
      #?(:clj source
         :cljs (to-json source)))))

(defn compose
  "Creates a normalized playable track from either a parsed AST or a UTF-8 string of bach data.
   A 'playable' track is formatted so that it is easily iterated over by a high-level Bach engine."
  [track]
  (cond
    (vector? track) (provision track)
    (string? track) (-> track parse provision)
    :else (problem "Cannot compose track, provided unsupported data format. Must be a parsed AST vector or a UTF-8 encoded string.")))
