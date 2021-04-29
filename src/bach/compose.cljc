; TODO: Refactor modules and functions based on protocols, so we don't rely on naming consistencies to fully express intent

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
            [clojure.core.memoize :refer [memo memo-clear!]]
            [bach.ast :as ast]
            [bach.math :refer [inverse-ratio]]
            [bach.track :refer :all]
            [bach.tree :refer :all]
            [bach.data :refer :all]))

(def uid #(nano-id 6))

(defn element-kind
  [elem]
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

(defn as-element
  "Creates a beat element from an :atom kind and collection of arguments.
   Parsed bach 'atoms' are considered 'elements', each having a unique id."
  [kind args]
  {:id (element-id kind)
   :kind (element-kind kind)
   :value (-> args first str)
   :props (rest args)})

(def as-element! (memo as-element))

(defn element-as-ids
  "Transforms normalized beat element(s) into their unique ids."
  [elems]
  (->> elems many (map :id)))

(defn beat-as-items
  "Provides all of the items in normalized beat(s) as a vector."
  [beats]
  (mapcat #(-> % :items vec) (many beats)))

(defn beat-as-elements
  "Provides all of the elements in normalized beat(s)."
  [beats]
  (->> beats many beat-as-items (mapcat :elements)))

(defn beat-as-element-ids
  "Provides all of the beat element item ids in normalized beat(s)."
  [beats]
  (->> beats many beat-as-elements (map :id)))

(defn index-beat-items
  "Adds the provided normalized beat's index to each of its items.
  Allows beat items to be handled independently of their parent beat."
  [beat]
  (->> beat :items
       (map #(assoc % :index (:index beat)))
       (sort-by :duration)))

(defn unit-context
  "Provides a standardized structure containing essential duration unit values.
  Primarily exists for allowing certain functions working with durations to
  access and optionally override these values without requiring a non-play tree."
  [tree]
  (let [tempo (get-tempo tree)
        meter (get-meter-ratio tree)
        beat (get-step-beat tree)]
    {:tempo tempo
     :meter meter
     :beat beat}))

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
  "Transforms a unitized duration tree into a 1-ary sequence quantized to the tree's greatest-common duration (i.e. step beat).
  In practice this enables uniform, linear and stateless interpolation of a N-ary duration tree."
  [tree]
  (->> tree (map as-reduced-durations) quantize))

(defn normalize-duration
  "Adjusts a beat's duration from being based on whole notes (i.e. 1 = 4 quarter notes) to being based on the provided unit beat (i.e. the duration of a single normalized beat, in whole notes).
  In general, this determines 'How many beats` does the provided duration equate to in this meter?'."
  ([duration units]
   (normalize-duration duration (:beat units) (:meter units)))
  ([duration beat meter]
   (let [inverse-beat (inverse-ratio #?(:clj (rationalize beat) :cljs beat))
         inverse-meter (inverse-ratio #?(:clj (rationalize meter) :cljs meter))
         within-bar? (<= duration meter)]
     ; TODO: Probably remove if condition, shouldn't be necessary
     (if within-bar?
       (/ duration beat)
       (* duration (max inverse-beat inverse-meter))))))

(def unitize-duration normalize-duration)

(defn- normalize-loop-iteration
  "Normalizes :when nodes in :loop AST tree at a given iteration.
  Replaces :when node with iter (as int) if :when expression matches
  the target iteration.
  Nilifies :when nodes that do not match the target iteration."
  [total iter tree]
  (insta/transform
    {:range #(when (some #{iter} (range %1 (+ 1 %2))) iter)
     :when-all (fn [& [:as all]]
                 (when (every? #{iter} (many all)) iter))
     :when-any (fn [& [:as all]]
                 (when (some #{iter} (many all)) iter))
     :when-match #(case (keyword %)
                    :even (when (even? iter) iter)
                    :odd (when (odd? iter) iter)
                    :last total
                    :first 1)
     :when-comp #(case (keyword %1)
                   :gt (when (> iter %2) iter)
                   :gte (when (>= iter %2) iter)
                   :lt (when (< iter %2) iter)
                   :lte (when (<= iter %2) iter)
                   :factor (when (= 0 (mod iter %2)) iter))
     :when-not #(when (not (= iter %)) iter)
     :when #(when (= iter %1) (many %2))}
     tree))

(defn- normalize-loop
  "Normalizes :loop AST tree by multiplying the tree's range by the number
  of iterations/loops and processing :when nodes."
  [iters tree]
  (->> (range iters)
       (mapcat #(normalize-loop-iteration iters (+ 1 %) (rest tree)))
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
  "Normalizes all bach collections in parsed AST as native Clojure structures.
  Enables pragmatic handling of trees and colls in subsequent functions.
  Input: [:list :a [:set :b :c] [:set :d [:list :e :f]]]
  Ouput: [:a #{:b :c} #{:d [:e :f]}]"
  [tree]
  (->> tree
    resolve-values
    normalize-loops
    (insta/transform
      {:list (fn [& [:as all]] (-> all collect vec))
       :set (fn [& [:as all]] (->> all collect (into #{})))
       :atom (fn [[_ kind] [_ & args]] (as-element! kind args))
       :beat #(assoc {} :duration %1 :elements (many %2))})))

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
  (-> tree transpose-collections squash))

(defn unitize-collections
  "Linearizes and normalizes every element's :duration in parsed AST against the unit within a meter.
  Establishes 'step beats' (or q-steps) as the canonical unit of iteration and indexing.
  In practice this ensures all durations are integers and can be used for indexing and quantized iteration."
  ([tree]
   (let [track (digest tree)
         units (unit-context track)]
     (unitize-collections track units)))
  ([tree units]
    (->> tree
        linearize-collections
        (cast-tree
          #(and (map? %) (:duration %))
          #(let [duration (int (unitize-duration (:duration %) units))]
             (assoc % :duration duration))))))

(defn itemize-beats
  "Transforms N-ary beat tree into a 1-ary sequence where each element is a map
  containing the beat's item(s) (as a set), duration (in q-steps) and index (in q-steps).
  Assumes beat collections are normalized and all durations are integers (used for indexing)."
  [beats]
  (let [durations (map as-reduced-durations beats)
        indices (linearize-indices durations)]
    (map #(assoc {} :items (-> %1 many set) :duration %2 :index %3) beats durations indices)))

; TODO: Probably just remove and rename position-beats to this
(defn linearize-beats
  [tree]
  (-> tree linearize-collections itemize-beats))

(defn normalize-beats
  "Normalizes beats in parsed AST for linear uniform iteration by decorating them
  with durations and indices based on a :unit (q-step by default) within a :meter.
  Note that the resulting format, designed for sequencing in consumers, is no longer hiccup."
  ([tree]
    (let [units (unit-context tree)]
      (normalize-beats tree units)))
  ([tree units]
   (-> tree (unitize-collections units) itemize-beats)))

(def normalize-beats! (memo normalize-beats))

(defn step-beat-signals
  "Transforms a parsed AST into a quantized sequence (in q-steps) where each step beat contains
  the index of its associated normalized beat (i.e. intersecting the beat's quantized duration)."
  ([tree]
    (step-beat-signals tree (unit-context tree)))
  ([tree units]
   (-> tree (unitize-collections units) quantize-durations)))

(defn element-play-signals
  "Provides a quantized sequence (in q-steps) of normalized beats where each step beat contains
  the id of every element that should be played at its index."
  [beats]
  (mapcat (fn [beat]
            (let [items (index-beat-items beat)
                  elems (beat-as-element-ids beat)]
              (cons elems (take (- (:duration beat) 1) (repeat nil))))) beats))

(defn element-stop-signals
  "Provides a quantized sequence (in q-steps) of normalized beats where each step beat contains
  the id of every element that should be stopped at its index."
  [beats]
  (let [duration (as-reduced-durations beats)
        items (mapcat index-beat-items beats)
        signals (-> duration (take (repeat nil)) vec)]
    (reduce
      (fn [acc item]
        (let [index (cyclic-index duration (+ (:index item) (:duration item)))
              item-elems (many (element-as-ids (:elements item)))
              acc-elems (many (get acc index))
              elems (concat item-elems acc-elems)]
          (assoc acc index (distinct elems)))) signals items)))

(defn provision-signals
  "Provisions quantized :play and :stop signals for every beat element in the tree.
  Enables state-agnostic and declarative event handling of beat elements by consumers."
  ([tree] (provision-signals tree (unit-context tree)))
  ([tree units]
   (let [beats (normalize-beats! tree units)
         beat-sigs (step-beat-signals tree units)
         play-sigs (element-play-signals beats)
         stop-sigs (element-stop-signals beats)]
     {:beat beat-sigs
      :play play-sigs
      :stop stop-sigs})))

; ALT: Itemized single-array structure (more characters, less access-time complexity O(1))
; (defn provision-signals
;   "Provisions quantized :beat, :play and :stop signals for every playable element in the tree.
;   Enables state-agnostic and declarative event handling of beat elements by consumers."
;   ([tree] (provision-signals tree (unit-context tree)))
;   ([tree units]
;    (let [beats (normalize-beats! tree units)
;          beat-sigs (step-beat-signals tree units)
;          play-sigs (element-play-signals beats)
;          stop-sigs (element-stop-signals beats)]
;      (map #(assoc-if {} {:beat %1 :play %2 :stop %3}) beat-sigs play-sigs stop-sigs))))

(defn provision-elements
  "Groups all normalized beats' elements and their values by `kind`.
  Allows consumers to directly resolve elements keyed by their uid."
  [beats]
  (->>
    beats
    beat-as-elements
    (reduce
      (fn [acc element]
        (let [uid (element-uid element)
              kind (element-kind element)
              data (select-keys element [:value :props])]
          (if (empty? element)
            acc
            (assoc-in acc [kind uid] data)))) {})))

(defn- provision-beat-items
  "Provisions a single normalized beat's item(s) for serialization and playback
  by casting their elements into ids."
  [items]
  (->> (many items)
       (map #(assoc % :elements (-> % :elements element-as-ids)))
       (sort-by :duration)))

(defn- provision-beat
  "Provisions a single normalized beat and its items for serialization and playback."
  [beat]
  (assoc beat :items (-> beat :items provision-beat-items)))

(defn provision-beats
  "Provisions normalized beat(s) for serialization and playback, replacing each
  beat element with its identifier string."
  [beats]
  (map provision-beat (many beats)))

; WARN: Migrate to `get-pulse-beat` once ready!
(defn provision-units
  "Provisions essential and common unit values of a track for serialization and playback."
  [track]
  (let [beat-units {:step (get-step-beat track)
                    :pulse (get-pulse-beat track)}
        bar-units  {:step (get-step-beats-per-bar track)
                    :pulse (get-pulse-beats-per-bar track)}
        time-units {:step (get-step-beat-time track)
                    :pulse (get-pulse-beat-time track)}]
   {:beat beat-units
    :bar  bar-units
    :time (assoc time-units :bar
                 (* (time-units :step) (bar-units :step)))}))

(defn provision-metrics
  "Provisions basic metric values of step beats in a track that are useful for playback."
  [beats]
  (let [durations (-> beats as-durations flatten)]
    {:min (apply min durations)
     :max (apply max durations)
     :total (as-reduced-durations beats)}))

(defn provision-headers
  "Provisions essential and user-provided headers of a track for serialization and playback."
  [track]
  (let [headers (get-headers track)
        ; tempo
        meter (get-meter track)]
    (assoc headers :meter meter)))


; TODO: Use keyword args to allow custom flexibile provisioning
;  - Also consider proposed Config! operator here, which would be used to control what gets provisioned and to inform engine so it can adapt its interpretation.
(defn provision
  "Provisions a track AST for high-level interpretation and playback."
  [data]
  (let [tree (parse data)
        track (playable identity tree)
        units (unit-context tree)
        beats (normalize-beats! track units)
        source {:iterations (get-iterations tree)
                :headers (provision-headers tree)
                :units (provision-units tree)
                :metrics (provision-metrics beats)
                :elements (provision-elements beats)
                :signals (provision-signals track units)
                :beats (provision-beats beats)}]
    #?(:clj source
        :cljs (to-json source))))

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
