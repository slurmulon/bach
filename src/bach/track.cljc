(ns bach.track
  (:require [instaparse.core :as insta]
            [nano-id.core :refer [nano-id]]
            [hiccup-find.core :refer [hiccup-find]]
            [clojure.core.memoize :refer [memo memo-clear!]]
            [bach.ast :as ast]
            [bach.data :refer :all]))

(def default-tempo 120)
(def default-meter [4 4])
; REMOVE
(def default-beat-unit (/ 1 (last default-meter)))
; REMOVE
(def default-pulse-beat default-beat-unit)

; REMOVE
(def default-headers {:tempo default-tempo
                      :meter default-meter})

(def valid-divisors (take 9 powers-of-two))
(def valid-max-tempo 256)
(def valid-max-duration 1024)

(declare resolve-values)

; find-headers
(defn get-headers
  "Provides the headers (aka meta info) for a parsed track"
  [track]
  (let [headers (atom default-headers)]
    (insta/transform
     {:header (fn [[& kind] value]
                (let [header-key (-> kind name clojure.string/lower-case keyword)]
                  (swap! headers assoc header-key value)))}
     (resolve-values track))
    @headers))

; get-header
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
  "Determines the main Play! export of a parsed track."
  [track]
  (-> track find-plays last))

(defn get-iterations
  "Determines how many iterations (loops) a parsed track is played for.
  Returns nil if the track loops forever (i.e. doesn't export Play! using a loop)."
  [track]
  (when-let [[tag iters] (get-play track)]
    (if (= tag :loop) iters nil)))

(defn get-tempo
  "Determines the global tempo of the track. Localized tempos are NOT supported yet."
  [track]
  (find-header track #(re-find #"(?i)tempo" %) default-tempo))

(defn get-meter
  "Determines the global meter, or time signature, of the track. Localized meters are NOT supported yet."
  [track]
  (find-header track #(re-find #"(?i)meter" %) default-meter))

(defn get-meter-ratio
  "Determines the global meter ratio of the track.
   For example: The ratio of the 6|8 meter is 3/4."
  [track]
  (let [[beats-per-measure & [beat-unit]] (get-meter track)]
    (/ beats-per-measure beat-unit)))

(defn get-pulse-beat
  "Determines the reference unit to use for beats, based on time signature.
  In music theory this is also known as the 'pulse beat' of the meter."
  [track]
  (/ 1 (last (get-meter track)))) ; AKA 1/denominator

; TODO: Refactor to accept normalized beats instead of track
(defn get-step-beat
  "Determines the greatest common beat (by duration) among every beat in a track.
  Once this beat is found, a track can be iterated through uniformly and without
  variance via linear interpolation, monotonic timers, intervals, etc.
  This normalized beat serves as the fundamental unit of iteration for bach engines."
  [track]
  ; FIXME: Use ##Inf instead in `lowest-duration` once we upgrade to Clojure 1.9.946+
  ; @see: https://cljs.github.io/api/syntax/Inf
  (let [lowest-duration (atom 1024)]
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
      (if step-beat-aligns?
        (min step-beat meter)
        (min step-beat-unit pulse-beat)))))

(defn get-pulse-beats-per-bar
  "Determines how many beats are in a bar/measure, based on the time signature."
  [track]
  (first (get-meter track)))

(defn get-step-beats-per-bar
  "Determines how many beats are in a bar/measure, normalized against the step beat of the track."
  [track]
  (let [step-beat (get-step-beat track)
        meter (get-meter-ratio track)]
    (safe-ratio
     (max step-beat meter)
     (min step-beat meter))))

; (def get-pulse-beats-per-measure get-normalized-beats-per-measure)

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
  (let [[_ & [beat-unit]] (get-meter track)]
    (if (not (some #{beat-unit} (rest valid-divisors)))
      (problem "Meter unit beats (i.e. step beats) must be even and no greater than " (last valid-divisors))
      true)))

(defn valid-play?
  "Determines if a parsed track has a single Play! export."
  [track]
  (if (not (= 1 (count (find-plays track))))
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
                   (problem (str "Beat values can only be an atom or set but found: " tag))
                 (->> value (hiccup-find [:list :loop]) empty? not)
                   (problem "Beats cannot contain a nested list or loop")))
       :div (fn [[& n] [& d]]
              (when (not (some #{d} valid-divisors))
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
(def resolve-variables deref-variables)

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
    ; TODO: Determine if this is necessary with our math grammar (recommended in instaparse docs)
    ; :expr identity,
    :string #(clojure.string/replace % #"^(\"|\')|(\"|\')$" "")} track))
(def resolve-values reduce-values)

(defn reduce-iterations
  "Reduces global loops (i.e. terations) of a parsed track's Play! into a list.
  Prevents redundant beats from being generated, hoisting 'iterations' to consumers."
  [track]
  (if (-> track get-iterations number?)
    (insta/transform
      {:play (fn [play]
               (let [values (rest (last play))]
                 [:play (into [:list] values)]))}
      track)
    track))

(defn reduce-track
  "Resolves variables and reduces primitive values in a parsed track into native Clojure structs."
  [track]
  (-> track
      resolve-variables
      resolve-values
      reduce-iterations))
(def digest reduce-track)

(defn parse
  "Consumes a hiccup AST or a UTF-8 string of bach data and produces a validated track tree.
  Resulting track tree is NOT valid hiccup, and is designed for internal use in bach.compose."
  [tree]
  (let [track (digest tree)]
    (when (valid? track) track)))
