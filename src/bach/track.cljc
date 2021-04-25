(ns bach.track
  (:require [instaparse.core :as insta]
            [nano-id.core :refer [nano-id]]
            [hiccup-find.core :refer [hiccup-find]]
            [clojure.core.memoize :refer [memo memo-clear!]]
            [bach.ast :as ast]
            [bach.math :refer [gcd powers-of-two safe-ratio]]
            [bach.tree :refer [hiccup-query]]
            [bach.data :refer :all]))

(def default-tempo 120)
(def default-meter [4 4])
(def default-headers {:tempo default-tempo :meter default-meter})

(def valid-divisors (take 9 powers-of-two))
(def valid-max-tempo 256)
(def valid-max-duration 1024)

(declare resolve-values)

; TODO: Use (hiccup-find [:headers] %) instead
(defn get-headers
  "Provides the headers (aka meta info) for a parsed track"
  [track]
  (let [headers (atom default-headers)]
    (insta/transform
     {:header (fn [[& [_ kind]] value]
                (let [header-key (-> kind name clojure.string/lower-case keyword)]
                  (println "---- get headersss?" header-key)
                  (swap! headers assoc header-key value)))}
     (resolve-values track))
    @headers))

(defn get-header
  "Generically finds a header entry in a parsed track by regex pattern."
  ([track pattern] (get-header track pattern nil))
  ([track pattern default]
   (let [header (atom default)]
     (insta/transform
       {:header (fn [[& [_ kind]] value]
                  (when (re-matches pattern (name kind))
                    (println "get header???" (name kind) pattern)
                    (reset! header value)))}
     (resolve-values track))
    @header)))

(defn get-plays
  "Finds and all of the Play! trees in a track."
  [track]
  (hiccup-query [:play] track))

(defn get-play
  "Determines the main Play! export of a parsed track."
  [track]
  (-> track get-plays last))

(defn get-iterations
  "Determines how many iterations (loops) a parsed track is played for.
  Returns nil if the track loops forever (i.e. doesn't export Play! using a loop)."
  [track]
  (when-let [[tag iters] (get-play track)]
    (if (= tag :loop) iters nil)))

(defn get-tempo
  "Determines the global tempo of the track. Localized tempos are NOT supported yet."
  [track]
  (get-header track #"(?i)tempo" default-tempo))

(defn get-meter
  "Determines the global meter, or time signature, of the track. Localized meters are NOT supported yet."
  [track]
  (get-header track #"(?i)meter" default-meter))

(defn meter-as-ratio
  [meter]
  (when-let [[beats-per-bar & [beat-unit]] meter]
    (/ beats-per-bar beat-unit)))

(defn get-meter-ratio
  "Determines the global meter ratio of the track.
   For example: The ratio of the 6|8 meter is 3/4."
  [track]
  (-> track get-meter meter-as-ratio))
  ; (let [[beats-per-measure & [beat-unit]] (get-meter track)]
  ;   (/ beats-per-measure beat-unit)))



(defn get-pulse-beat
  "Determines the reference unit to use for beats, based on time signature.
  In music theory this is also known as the 'pulse beat' of the meter."
  [track]
  (/ 1 (last (get-meter track)))) ; AKA 1/denominator

(defn get-step-beat
  "Determines the greatest common beat (by duration) among every beat in a track.
  Once this beat is found, a track can be iterated through uniformly and without
  variance via linear interpolation, monotonic timers, intervals, etc.
  This normalized beat serves as the fundamental unit of iteration for bach engines."
  [tree]
  ; FIXME: Use ##Inf instead in `lowest-duration` once we upgrade to Clojure 1.9.946+
  ; @see: https://cljs.github.io/api/syntax/Inf
  (let [lowest-duration (atom 1024)
        track (resolve-values tree)]
    (insta/transform
     {:beat (fn [duration _]
              (when (< duration @lowest-duration)
                (reset! lowest-duration duration)))}
     track)
    (let [pulse-beat (get-pulse-beat track)
          meter (get-meter-ratio track)
          step-beat @lowest-duration
          step-beat-unit (gcd step-beat meter)
          step-beat-aligns? (= 0 (mod (max step-beat meter)
                                      (min step-beat meter)))]
      (println "\n\n---- step beat aligns" step-beat-aligns? step-beat meter)
      (if step-beat-aligns?
        (min step-beat meter)
        (min step-beat-unit pulse-beat)))))

(defn get-pulse-beats-per-bar
  "Determines how many beats are in a bar/measure, based on the time signature."
  [track]
  (first (get-meter track)))

(defn get-step-beats-per-bar
  "Determines how many beats are in a bar/measure, normalized to the step beat of the track."
  [track]
  (let [step-beat (get-step-beat track)
        meter (get-meter-ratio track)]
    (safe-ratio
     (max step-beat meter)
     (min step-beat meter))))

; TODO: Write tests
(defn get-pulse-beat-time
  "Determines the number of milliseconds equal to a one pulse-beat duration."
  [track]
  (let [tempo (get-tempo track)
        beats-per-second (/ tempo 60)
        seconds-per-beat (/ 1 beats-per-second)
        ms-per-beat (* seconds-per-beat 1000)]
    (float ms-per-beat)))

(defn get-step-beat-time
  "Determines the number of milliseconds equal to one step-beat duration."
  [track]
  (let [pulse-beat-time (get-pulse-beat-time track)
        pulse-beat (get-pulse-beat track)
        step-beat (get-step-beat track)
        step-pulse-beat-ratio (/ step-beat pulse-beat)
        step-beat-time (* pulse-beat-time step-pulse-beat-ratio)]
    (float step-beat-time)))

(defn variable-scope
  "Provides a localized scope/stack for tracking variables."
  [scope]
  (let [context (atom {})]
    (letfn [(variables []
              (get @context :vars {}))
            ; TODO: might just want to move this into `dereference-variables`
            (set-variable! [label value]
              (swap! context assoc :vars (conj (variables) [label value])))]
      (scope variables set-variable! context))))

(defn valid-tempo?
  "Determines if a parsed track has a valid tempo."
  [track]
  (let [tempo (get-tempo track)]
    (if (not (<= 0 tempo valid-max-tempo))
      (problem "Tempos must be between 0 and " valid-max-tempo " beats per minute")
      true)))

(defn valid-meter?
  "Determines if a parsed track has a valid meter, particularly its pulse beat."
  [track]
  (let [[_ & [unit-beat]] (get-meter track)]
    (if (not (some #{unit-beat} (rest valid-divisors)))
      (problem "Meter unit beats (i.e. pulse beats) must be even and no greater than " (last valid-divisors))
      true)))

(defn valid-play?
  "Determines if a parsed track has a single Play! export."
  [track]
  (if (not (= 1 (count (get-plays track))))
    (problem "Exactly one Play! export must be defined")
    true))

(defn valid-resolves?
  "Determines if a parsed track's resolves values are valid or not."
  [track]
  (variable-scope
   (fn [variables set-variable! _]
     (insta/transform
       {:assign (fn [[& label] [& [value-type] :as value]]
                  (case value-type
                    :identifier
                    (when (-> (variables) (contains? value) not)
                      (problem (str "Variable is not declared before it's used: " value)))
                    (set-variable! label value)))
       :beat (fn [duration [tag :as value]]
               (cond
                 (> duration valid-max-duration)
                   (problem (str "Beat durations must be between 0 and " valid-max-duration))
                 (compare-items not-every? [:atom :set] tag)
                   (problem (str "Beat values can only be an atom or set but found: " tag))))
       :div (fn [_ [& div]]
              (when (not (some #{div} valid-divisors))
                (problem "All divisors must be even and no greater than " (last valid-divisors))))}
      track)))
  true)

; TODO
; validate-no-cycles
; validate-no-negative-durations

(defn validate
  "Determines if a parsed track passes all validation rules."
  [track]
  (->> [valid-resolves? valid-tempo? valid-meter? valid-play?]
      (map #(% track))
      (every? true?)))

(def valid? validate)
(def validate! (memo validate))

(defn resolve-variables
  "Dereferences and resolves any variables found in the parsed track.
  Does NOT support hoisting (yet)."
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
                       (if (some? stack-value) stack-value [:identifier label])))
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
  "Reifies all primitive values in a parsed track into native Clojure types."
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
    :string #(clojure.string/replace % #"^(\"|\')|(\"|\')$" "")} track))
(def resolve-values reduce-values)

; FIXME: Probably remove for now, since this prevents `when` from being used if the main export of Play! is a loop
(defn reduce-iterations
  "Reduces global loops (i.e. terations) of a parsed track's Play! into a list.
  Prevents redundant beats from being generated, hoisting 'iterations' to consumers."
  [track]
  (if (-> track get-iterations number?)
    (insta/transform
      {:play #(let [values (rest (last %))]
                [:play (into [:list] values)])}
      track)
    track))

(defn digest
  "Resolves variables and reduces primitive values in a parsed track into native Clojure structs."
  [track]
  (-> track
      resolve-variables
      resolve-values
      reduce-iterations))

(defn parse
  "Consumes a hiccup tree and produces a validated track tree (throws if an exception).
  Resulting track tree is NOT valid hiccup, and is designed for internal use in bach.compose."
  [tree]
  (let [track (digest tree)]
    (when (valid? track) track)))

; FIXME: May need to have this return a map instead with :headers and :play, to avoid losing data while passing around funcs
; TODO (either):
;  - Redundantly/safely call reduce-values or the like in each compose method that requires it
;  - Pattern match against a context object in addition to custom `_ unit meter` args.
(defn playable
  "Parses a track (hiccup tree) and returns the tree of the main Play! export."
  [track]
  (-> track parse get-play))
  ; FAIL FIX: Instead should return simple map here, as its harder to transform the tree into an ideal structure for composition (it really just wants to work with the exported play tree and nothing else).
  ; [tree]
  ; (let [track (parse tree)
  ;       ; headers (get-headers track)
  ;       headers (hiccup-find [:header] track)
  ;       play (get-play track)]
  ;   (conj [:track]
  ;         (into [:statement] headers)
  ;         (conj [:statement] play))))
    ; {:headers headers
    ;  :play play}))
