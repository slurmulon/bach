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
            [nano-id.core :refer [nano-id]]
            [hiccup-find.core :refer [hiccup-find]]
            [clojure.core.memoize :refer [memo memo-clear!]]
            ; [bach.ast :refer [parse]]
            [bach.ast :as ast]
            [bach.data :refer :all]))

(def default-tempo 120)
(def default-meter [4 4])
(def default-beat-unit (/ 1 (last default-meter)))
(def default-pulse-beat default-beat-unit)

(def default-headers {:tempo default-tempo
                      :meter default-meter})

(def default-units {:beat-unit default-beat-unit
                    :pulse-beat default-pulse-beat
                    :beat-units-per-measure 4
                    :pulse-beats-per-measure 4
                    :total-beats 0
                    :total-beat-units 0
                    :total-pulse-beats 0
                    :ms-per-pulse-beat 0
                    :ms-per-beat-unit 0})

(def valid-divisors (take 9 powers-of-two))
(def valid-max-tempo 256)
(def valid-max-duration 1024)

(def uid #(nano-id 6))

(defn element-kind
  [elem]
  ; (println "elem kind???")
  ; (clojure.pprint/pprint elem)
  (if (map? elem)
    (-> elem :kind element-kind)
    (-> elem name clojure.string/lower-case keyword)))

(defn element-id
  [elem]
  (if (map? elem)
    (:id elem)
    (str (-> elem element-kind name) "." (uid))))

(defn element-uid
  [elem]
  (-> elem element-id (clojure.string/split #"\.") last))

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

; TODO: Move to bach.ast or bach.data
(defn- compare-tags
  [is? in tags]
  (is? (zipmap in (repeat true)) (many tags)))

; (defn every-tag?
;   [in tags]
;   (compare-tags every? in tags))
;   ; (every? (zipmap coll (repeat true)) (many tags)))

; (defn some-tag?
;   [coll tag]
;   (compare-tags some in tags))
  ; (some (zipmap coll (repeat true)) (many tags)))

; (def not-every-tag? #(not (every-tag? %1 %2)))
; (def not-some-tag? #(not (some-tag? %1 %2)))

; TODO: integreate reduce-values
; TODO: check for :play
; TODO: validate any variables in :play
(defn validate
  "Determines if a parsed track is valid or not."
  [track]
  (variable-scope
   (fn [variables create-variable _]
     (insta/transform
       {:assign (fn [[& label] [& [value-type] :as value]]
                  (case value-type
                    :identifier
                    (when (-> (variables) (contains? value) not)
                      (problem (str "Variable is not declared before it's used: " value)))
                    (create-variable label value)))
       :pair (fn [duration [tag :as value]]
               (cond
                 (> duration valid-max-duration)
                   (problem (str "Beat durations must be between 0 and " valid-max-duration))
                 (compare-tags not-every? [:atom :set] tag)
                   (problem (str "Beat values can only be an atom or set but found: " tag))
                 (->> value (hiccup-find [:list :loop]) empty? not)
                   (problem "Beats cannot contain a nested list or loop")))
       :div (fn [[& n] [& d]]
              (when (not (some #{d} valid-divisors))
                (problem "All divisors must be even and no greater than " (last valid-divisors))))}
       ; TODO: Re-implement, :tempo is no longer an AST tag
       ; :tempo (fn [& tempo]
       ;          (when (not (<= 0 tempo valid-max-tempo))
       ;            (problem "Tempos must be between 0 and " valid-max-tempo " beats per minute")))}
      track)))
  true)

(def valid? validate)
(def validate! (memo validate))

; TODO
; validate-ast
; validate-headers
; validate-play
; validate-divisors
; validate-tempo
; validate-no-cycles

; (defn validate-2
;   [track]
;   (->> [validate-ast validate-headers validate-play]
;       (map #(% track))
;       (every? true?)))

(defn deref-variables
  "Dereferences any variables found in the parsed track. Does NOT support hoisting (yet)."
  [track]
  (variable-scope
   (fn [variables set-variable! _]
     (insta/transform
      {:assign (fn [label-token value-token]
                 (let [label (last label-token)
                       value (last value-token)
                       [value-type] value-token]
                   (case value-type
                     :identifier
                     (let [stack-value (get (variables) value)]
                       (set-variable! label stack-value)
                       [:assign label-token stack-value])
                     (do (set-variable! label value-token)
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
    :int to-string,
    :float to-string,
    :name to-string,
    ; TODO: Determine if this is necessary with our math grammar (recommended in instaparse docs)
    ; :expr identity,
    :string #(clojure.string/replace % #"^(\"|\')|(\"|\')$" "")} track))
(def parse-values reduce-values)
(def normalize-values reduce-values)

(defn reduce-track
  "Dereferences variables and reduces the primitive values in a parsed track."
  [track]
  (-> track
      deref-variables
      reduce-values))
; (def normalize-values reduce-track)
(def digest reduce-track)

; defn consume
(defn parse
  [tree]
  (let [track (digest tree)]
    (when (valid? track) track)))
  ; (when (valid? track)
  ;   (digest track)))

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
     {:header (fn [[& kind] value]
                (let [header-key (-> kind name clojure.string/lower-case keyword)]
                  (swap! headers assoc header-key value)))}
     reduced-track)
    @headers))

(defn find-header
  "Generically finds a header entry / meta tag in a parsed track by its label"
  [track label default]
  (let [header (atom default)]
    (insta/transform
     {:header (fn [[& kind] value]
                (when (= (str kind) (str label))
                  (reset! header value)))}
     track)
    @header))

(defn find-plays
  "Finds and all of the Play! trees in a track."
  [track]
  (hiccup-find-trees [:play] track))

(defn get-play
  "Determines the main Play! export of the track."
  [track]
  (-> track find-plays last))

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

; ------ V3 --------

; craft-element
(defn as-element
  "Creates an element from an :atom kind and collection of arguments.
   Parsed bach 'atoms' are considered 'elements', each having a unique id."
  [kind args]
  {:id (element-id kind)
   :kind (element-kind kind)
   :value (-> args first str)
   :props (rest args)})

(def as-element! (memo as-element))

(defn- normalize-loop-whens
  "Normalizes :when nodes in :loop AST tree at a given iteration.
  Nilifies :when nodes that do not occur at the target iteration."
  [iter tree]
  (insta/transform {:when #(when (= iter %1) (many %2))} tree))

(defn- normalize-loop
  "Normalizes :loop AST tree by multiplying the tree's range by the number
  of iterations/loops and processing :when nodes."
  [iters tree]
  (->> (range iters)
       (mapcat #(normalize-loop-whens (+ 1 %) (rest tree)))
       (filter (complement nil?))))

(defn normalize-loops
  "Normalizes :loops in AST tree by expanding the loop's items into a new AST collection tree.
  Uses the keyword of the loop's value for the new collection type.
  Input: [:loop 2 [:list :a :b :c]]
  Ouput: [:list :a :b :c :a :b :c]"
  [tree]
  (insta/transform {:loop #(into [(first %2)] (normalize-loop %1 %2))} tree))

; TODO: Detect cyclic references!
;  - Should do this more generically in `reduce-values` or the like, instead of here
; TODO: Rename to normalize-collections or reduce-collections
(defn normalize-collections
  "Normalizes all bach collections in parsed AST as native clojure structures.
  Enables pragmatic handling of trees and colls in subsequent functions.
  Input: [:list :a [:set :b :c] [:set :d [:list :e :f]]]
  Ouput: [:a #{:b :c} #{:d [:e :f]}]"
  [tree]
  (->> tree
    normalize-values
    normalize-loops
    (insta/transform
      {:list (fn [& [:as all]] (-> all collect vec))
       :set (fn [& [:as all]] (->> all collect (into #{})))
       :atom (fn [[_ kind] [_ & args]] (as-element! kind args))
       ; FIXME: Refactor towards this!
       ;  - Currently prevents `2 -> { Chord('A')`
       :pair #(assoc {} :duration %1 :elements (many %2))})))
       ; :pair #(assoc {} :duration %1 :elements %2)})))

(defn as-durations
  "Transforms each node in a tree containing a map with a :duration into
  its associated scalar value. Assumes :duration values are numeric."
  [tree]
  (cast-tree map? :duration tree))

(defn reduce-durations
  "Transforms a tree of numeric duration nodes into a single total duration
  according to bach's nesting rules (max of sets, sum of seqentials)."
  [tree]
  (clojure.walk/postwalk
    #(cond
       (set? %) (flatten-by max (seq %))
       (sequential? %) (flatten-by + %)
       :else %)
    tree))

(defn as-reduced-durations
  "Transforms a parsed AST into a homogenous duration tree, then reduces it."
  [tree]
  (->> tree as-durations reduce-durations))

(defn quantize-durations
  "Transforms a unitized duration tree into a 1-ary sequence quantized to the tree's greatest-common duration.
  In practice this enables uniform, linear and stateless interpolation of a N-ary duration tree."
  [tree]
  (->> tree (map as-reduced-durations) stretch))

; TODO: Probably just remove
(defn normalize-durations
  [tree]
  (->> tree normalize-collections as-reduced-durations))

(defn transpose-collections
  "Aligns and transposes parallel collection elements of a parsed AST, enabling linear time-invariant iteration by consumers.
   Each item of the resulting sequence is a set containing all of the elements occuring at its time/column-index, across all parallel/sibling vectors.
   This ensures that resulting set trees are order agnostic, only containing non-sequentials and other sets.
   Input: [#{[:a :b] [:c :d]} :e :f]
   Ouput: [[#{:a :c} #{:b :d}] :e :f]"
  [tree]
  (->> tree
       normalize-collections
       (cast-tree set?
         (fn [set-coll]
           (let [set-items (map #(if (sequential? %) (vec %) [%]) set-coll)
                 aligned-items (->> set-items transpose (mapv #(into #{} %)))]
             (if (next aligned-items) aligned-items (first aligned-items)))))))

(defn linearize-collections
  "Linearly transposes and flattens all collections in the parsed AST into a 1-ary sequence."
  [tree]
  (-> tree transpose-collections squash-tree))

(defn unitize-collections
  "Linearizes and normalizes every element's :duration in parsed AST against the unit within a meter.
  Establishes 'pulse beats' (or q-pulses) as the canonical unit of iteration and indexing.
  In practice this ensures all durations are integers and can be used for indexing and quantized iteration."
  ([tree]
    (let [track (reduce-track tree)
          unit (get-pulse-beat track)
          meter (get-meter-ratio track)]
      (unitize-collections track unit meter)))
  ([tree unit meter]
    (->> tree
        linearize-collections
        (cast-tree
          #(and (map? %) (:duration %))
          #(let [duration (int (normalize-duration (:duration %) unit meter))]
             (assoc % :duration duration))))))

(defn itemize-beats
  "Transforms N-ary beat tree into a 1-ary sequence where each element is a map
  containing the beat's item(s) (as a set), duration (in q-pulses) and index (in q-pulses).
  Assumes beat collections are normalized and all durations are integers (used for indexing)."
  [beats]
  (let [durations (map as-reduced-durations beats)
        indices (linearize-indices identity durations)]
    (map #(assoc {} :items (-> %1 many set) :duration %2 :index %3) beats durations indices)))

(def position-beats itemize-beats)

; TODO: Probably just remove and rename position-beats to this
(defn linearize-beats
  [tree]
  (-> tree linearize-collections itemize-beats))

(defn normalize-beats
  "Normalizes beats in parsed AST for linear uniform iteration by decorating them
  with durations and indices based on a unit (q-pulse by default) within a meter."
  ([tree]
    (let [track (reduce-track tree)
          unit (get-pulse-beat track)
          meter (get-meter-ratio track)]
      (normalize-beats track unit meter)))
  ([tree unit meter]
   (-> tree (unitize-collections unit meter) itemize-beats)))

(def normalize-beats! (memo normalize-beats))

(defn all-beat-items
  "Provides all of the items in a collection of normalized beats as a vector."
  [beats]
  (mapcat #(-> % :items vec) (many beats)))

(defn all-beat-elements
  "Provides all of the elements in a collection of normalized beats."
  [beats]
  (->> beats many all-beat-items (mapcat :elements)))

(defn all-beat-element-ids
  "Provides all of the beat element item ids in a collection of normalized beats."
  [beats]
  (->> beats many all-beat-elements (map :id)))

(defn cast-beat-element-ids
  "Transforms normalized beat element(s) into their unique ids."
  [elem]
  (->> elem many (map :id)))

(defn index-beat-items
  "Adds the provided normalized beat's index to each of its items.
  Allows beat items to be handled independently of their parent beat."
  [beat]
  (->> beat :items
       (map #(assoc % :index (:index beat)))
       (sort-by :duration)))

(defn pulse-beat-signals
  "Transforms a parsed AST into a quantized sequence (in q-pulses) where each pulse beat contains
  the index of its associated normalized beat (i.e. intersecting the beat's quantized duration)."
  [tree]
  (-> tree unitize-collections quantize-durations))

(defn element-play-signals
  "Provides a quantized sequence (in q-pulses) of normalized beats where each pulse beat contains
  the id of every element that should be played at its index."
  [beats]
  (mapcat (fn [beat]
            (let [items (index-beat-items beat)
                  elems (all-beat-element-ids beat)]
              (cons elems (take (- (:duration beat) 1) (repeat nil))))) beats))

(defn element-stop-signals
  "Provides a quantized sequence (in q-pulses) of normalized beats where each pulse beat contains
  the id of every element that should be stopped at its index."
  [beats]
  (let [duration (as-reduced-durations beats)
        items (mapcat index-beat-items beats)
        signals (-> duration (take (repeat nil)) vec)]
    (reduce
      (fn [acc item]
        (let [index (cyclic-index duration (+ (:index item) (:duration item)))
              item-elems (many (->> item :elements (map :id)))
              acc-elems (many (get acc index))
              elems (concat item-elems acc-elems)]
          (assoc acc index (distinct elems)))) signals items)))

(defn provision-elements
  "Groups all normalized beats' elements and their values by `kind`.
  Allows consumers to directly resolve elements keyed by their uid."
  [beats]
  (->>
    beats
    all-beat-elements
    (reduce
      (fn [acc element]
        (let [uid (element-uid element)
              kind (element-kind element)
              data (select-keys element [:value :props])]
          (if (empty? element)
            acc
            (assoc-in acc [kind uid] data)))) {})))

(defn provision-signals
  "Provisions quantized :play and :stop signals for every beat element in the tree.
  Enables state-agnostic and declarative event handling of beat elements by consumers."
  [tree]
  (let [beats (normalize-beats! tree)
        beat-sigs (pulse-beat-signals tree)
        play-sigs (element-play-signals beats)
        stop-sigs (element-stop-signals beats)]
    {:beat beat-sigs
     :play play-sigs
     :stop stop-sigs}))

(defn provision-beat-items
  "Provisions a single normalized beat's item(s) for serialization and playback
  by casting their elements into ids."
  [items]
  (->> (many items)
       (map #(assoc % :elements (-> % :elements cast-beat-element-ids)))
       (sort-by :duration)))

(defn provision-beat
  "Provisions a single normalized beat and its items for serialization and playback."
  [beat]
  (assoc beat :items (-> beat :items provision-beat-items)))

(defn provision-beats
  "Provisions normalized beat(s) for serialization and playback, replacing each
  beat element with its identifier string."
  [beats]
  (map provision-beat (many beats)))

; FIXME: Needs to be refactored in light of collection nesting, loops, whens, etc.
; FORMAT DRAFTS:
;   - TODO: Probably rename `unit` to `pulse` and `pulse` to `rhythm`
;     - Alternatively, could just have `beat` and `pulse` (old `unit`)
;     - OR just `rbeat` and `pbeat`, or just `r` and `p`
;       - `q` might be better than `r` (since `r` more relates to things like swing, which is not necessarily the quantized beat, hence `q`).
;       - `f` could also be better than `q`, for "foot" beat. (or even "frequency" beat)
;       - `s` could also be better than `q`, for "signal" or "step" beat.
;       - `w` could also be better than `q`, for "wave" beat.
;   - @see: https://en.wikipedia.org/wiki/Rhythm#Unit_and_gesture
; {
;   units: {
;     beat: {
;       step: 1/4
;       pulse: 1/4
;     }
;     bar: {
;       step: 4
;       pulse: 4
;     }
;     time: {
;       step: 500
;       pulse: 500
;       bar: 2000
;     }
;     total: {
;       step: 8
;       pulse: 8
;       bar: 2
;       time: 4000
;     }
;  }

(defn provision-units
  "Provisions essential and common unit values of a track for serialization and playback."
  [track]
  {;:total-beats (get-total-beats track)
   ;:total-beat-units (get-total-beat-units track)
   ; :total-pulse-beats (get-total-pulse-beats track)
   :total-pulse-beats (as-reduced-durations track)
   ;:beat-units-per-bar (get-beat-units-per-measure track)
   ;:pulse-beats-per-bar (get-pulse-beats-per-measure track)
   ;:ms-per-beat-unit (get-ms-per-beat track :unit)
   ;:ms-per-pulse-beat (get-ms-per-beat track :pulse)
   ; ms-per-bar
   ;:beat-unit (get-beat-unit track)
   ;:pulse-beat (get-pulse-beat track)})
   })

(defn provision-headers
  "Provisions essential and user-provided headers of a track for serialization and playback."
  [track]
  (let [headers (get-headers track)
        meter (get-meter track)]
    (assoc headers :meter meter)))

(defn playable
  "Parses a track and returns the AST tree of the main Play! export."
  [track]
  (-> track parse get-play))

; TODO: Use keyword args to allow custom flexibile provisioning
;  - Also consider proposed Config! operator here, which would be used to control what gets provisioned and to inform engine so it can adapt its interpretation.
(defn provision
  "Provisions a track for high-level interpretation and playback."
  [tree]
  (when-let [track (playable tree)]
    (let [beats (normalize-beats! track)
          headers (provision-headers track)
          ; units (provision-units track)
          units (provision-units beats)
          signals (provision-signals track)
          elements (provision-elements beats)
          data (provision-beats beats)
          source {:headers headers
                  :units units
                  :signals signals
                  :elements elements
                  ; TODO: Maybe rename to `rhythm` (wont' be necessary if we establish `rhythm` beat as the new `pulse` beat)
                  :beats data}]
      #?(:clj source
         :cljs (to-json source)))))

(defn compose
  "Creates a normalized playable track from either a parsed AST or a UTF-8 string of bach data.
   A 'playable' track is formatted so that it is easily iterated over by a high-level Bach engine."
  [track]
  (cond
    (vector? track) (provision track)
    (string? track) (-> track ast/parse provision)
    :else (problem "Cannot compose track, provided unsupported data format. Must be a parsed AST vector or a UTF-8 encoded string.")))

(def compose! (memo compose))

(defn clear!
  "Clears the cache state of all memoized functions."
  []
  (map memo-clear! [validate! as-element! normalize-beats!]))
