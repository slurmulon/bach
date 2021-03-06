; TODO: Also use multi-methods
;  - @see: https://www.braveclojure.com/multimethods-records-protocols/
; TODO: Refactor modules and functions based on protocols, so we don't rely on naming consistencies to fully express intent

(ns bach.compose
  (:require [instaparse.core :as insta]
            [nano-id.core :refer [nano-id]]
            [bach.ast :as ast]
            [bach.track :as tracks]
            [bach.math :refer [inverse-ratio]]
            [bach.tree :refer [cast-tree flatten-by flatten-one squash itemize quantize transpose linearize-indices hiccup-query]]
            [bach.data :refer [many collect expand compare-items assoc-if cyclic-index nano-hash to-json problem]]))

(def uid nano-hash)

(def serialize #?(:clj identity :cljs to-json))

(def ^:dynamic *unit-beat* (/ 1 4))

(defn element->kind
  [elem]
  (if (map? elem)
    (-> elem :kind element->kind)
    (-> elem name clojure.string/lower-case keyword)))

(defn element->id
  ([elem] (element->id elem elem))
  ([elem seed]
   (if (map? elem)
     (:id elem)
     (str (-> elem element->kind name) "." (uid seed)))))

(defn element->uid
  [elem]
  (-> elem element->id (clojure.string/split #"\.") last))

; TODO: defrecord Element

(defn make-element
  "Creates a beat element from an :atom kind and collection of arguments.
   Parsed bach 'atoms' are considered 'elements', each having a unique id."
  [kind args]
  {:id (element->id kind (into [kind] args))
   :kind (element->kind kind)
   :value (-> args first str)
   :props (rest args)})

(def ^:export elementize (comp serialize make-element))

(defn element-as-ids
  "Transforms normalized beat element(s) into their unique ids."
  [elems]
  (->> elems collect (map :id) sort))

(defn beat-as-items
  "Provides all of the items in normalized beat(s) as a vector."
  [beats]
  (mapcat #(-> % :items vec) (many beats)))

(defn beat-as-elements
  "Provides all of the elements in normalized beat(s)."
  [beats]
  (->> beats collect beat-as-items (mapcat :elements)))

(defn beat-as-element-ids
  "Provides all of the beat element item ids in normalized beat(s)."
  [beats]
  (->> beats collect beat-as-elements (map :id) sort))

(defn index-beat-items
  "Adds beat's step index to each item in a beat."
  [beat]
  (->> beat :items
       (map #(merge % (select-keys beat [:index :id])))))

(defn unit-beat
  "Establishes the unit (in whole notes) to standardize all beat durations as."
  ([] *unit-beat*)
  ([tree] (tracks/get-step-beat tree)))

(defn as-durations
  "Transforms each node in a tree containing a map with a :duration into
  its associated scalar value. Assumes :duration values are numeric."
  [tree]
  (cast-tree map? :duration tree))

(defn reduce-durations
  "Transforms a tree of numeric duration nodes into a single total duration
  according to bach's nesting rules (min of sets, sum of seqentials)."
  [tree]
  (clojure.walk/postwalk
   #(cond
      (set? %) (flatten-by min (collect %))
      (sequential? %) (flatten-by + (collect %))
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

(defn unitize-duration
  "Normalizes a duration value into an integer based on a unit value.
  In other words it determines how many units a duration equates to.
  In practice, this converts raw durations (in whole notes) into q-steps."
  ([duration] (unitize-duration duration *unit-beat*))
  ([duration unit] (int (/ duration unit))))

(defn unitize-durations
  [tree unit]
  (cast-tree
   #(and (map? %) (:duration %))
   #(let [duration (unitize-duration (:duration %) unit)]
      (assoc % :duration duration)) tree))

(defn- normalize-loop-iteration
  "Normalizes :when nodes in :loop AST tree at a given iteration.
  Replaces :when node with iter (as int) if :when expression matches
  the target iteration.
  Nilifies :when nodes that do not match the target iteration."
  [total iter tree]
  (insta/transform
   {:range #(when (some #{iter} (range %1 (inc %2))) iter)
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
       (mapcat #(normalize-loop-iteration iters (inc %) (rest tree)))
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
(defn normalize-collections
  "Normalizes all bach collections in parsed AST as native Clojure structures.
  Enables pragmatic handling of trees, colls and items in subsequent functions.
  Input: [:list :a [:set :b :c] [:set :d [:list :e :f]]]
  Ouput: [:a #{:b :c} #{:d [:e :f]}]"
  [tree]
  (->> tree
       tracks/resolve-values
       normalize-loops
       (insta/transform
        {:list (fn [& [:as all]] (with-meta (-> all collect vec) {:bach true}))
         :set (fn [& [:as all]] (with-meta (->> all collect (into #{})) {:bach true}))
         :atom (fn [[_ kind] [_ & args]] (make-element kind args))
         :rest #(make-element :rest [])
         :beat #(assoc {} :duration %1 :elements (vec (many %2)))})))

(def normalized-list?
  #(and (vector? %) (not (map-entry? %)) (contains? (meta %) :bach)))

(def normalized-set?
  #(and (set? %) (contains? (meta %) :bach)))

; TODO: Use normalized-set? here instead of set? (and test)
(defn transpose-sets
  "Transposes all sets containing a sequential in parsed AST tree."
  [tree]
  (cast-tree set?
             (fn [set-coll]
               (let [set-items (->> set-coll collect (map #(if (sequential? %) (vec %) [%])))
                     aligned-items (->> set-items transpose (mapv #(into #{} %)))]
                 (if (next aligned-items) aligned-items (first aligned-items))))
             tree))

(defn transpose-lists
  "Transposes all normalized bach lists (and any sets they contain) in parsed AST tree."
  [tree]
  (cast-tree
   clojure.walk/postwalk
   normalized-list?
   #(reduce
     (fn [acc item]
       (if-let [duration (:duration item)]
         (into acc (expand item duration))
         (cond
           (set? item)
           (if (every? (fn [x] (and (map? x) (:duration x))) item)
             (conj acc (expand item (as-reduced-durations item)))
             (conj acc item))
           (sequential? item) (into acc item)
           :else (conj acc item)))) [] %) tree))

; TODO: Probably rename to linearize-collections
(defn quantize-collections
  "Transforms parsed N-ary AST tree into a 1-ary sequence quantized to the unit/step duration."
  [tree unit]
  (-> (normalize-collections tree)
      (unitize-durations unit)
      transpose-lists
      transpose-sets
      squash))

(defn normalize-beats
  "Transforms N-ary beat tree into a 1-ary sequence where each element is a map
  containing the beat's item(s) (as a set), duration (in q-steps) and index (in q-steps).
  Assumes all durations are integers (used for indexing)."
  [tree unit]
  (let [steps (quantize-collections tree unit)
        beats (atom 0)]
    (map-indexed
     (fn [index beat]
       (when-not (empty? beat)
         (assoc {} :items (-> beat collect set)
                    ; TODO: Rename to :index
                :id (dec (swap! beats inc))
                :duration  (as-reduced-durations beat)
                    ; TODO: Rename to :step
                :index index)))
     steps)))

(defn provision-element-steps
  "Overlays/joins elements across quantized beats using the range of their durations.
  Enables consumers to determine which elements are playing on any step without needing
  to iterate or perform repetitive state calculations."
  ([tree]
   (provision-element-steps tree (unit-beat tree)))
  ([tree unit]
   ; TODO: Hoist out instead expect pre-normalized beats
   (let [beats (normalize-beats tree unit)
         items (mapcat index-beat-items beats)]
     (reduce
      (fn [result item]
        (let [index (:index item)
              elems (many (element-as-ids (:elements item)))
              span (range index (+ index (:duration item)))]
          (reduce #(assoc %1 %2 (sort-by str (into (get %1 %2) elems)))
                  result span)))
      [] items))))

(defn provision-beat-steps
  "Transforms a parsed AST into a quantized sequence (in q-steps) where each step beat contains
  the index of its associated normalized beat (i.e. intersecting the beat's quantized duration)."
  ([tree]
   (provision-beat-steps tree (unit-beat tree)))
  ([tree unit]
   ; TODO: Hoist out and instead expect pre-normalized beats
   (let [beats (normalize-beats tree unit)]
     (reduce
      (fn [acc beat]
        (conj acc (if (nil? beat) (peek acc) (:id beat))))
      [] beats))))

(defn provision-context-steps
  "Provisions and joins both beat and element steps into a single sequence.
   Allows consumers to determine, w/o iteration, which beat and elements are playing on a step.
   First element is the beat id, subsequent values are element ids playing on the step."
  ([tree]
   (provision-context-steps tree (unit-beat tree)))
  ([tree unit]
   (let [beats (provision-beat-steps tree unit)
         elems (provision-element-steps tree unit)]
     (map cons beats elems))))
(def provision-state-steps provision-context-steps)

(defn provision-play-steps
  "Provides a quantized sequence (in q-steps) of normalized beats where each step beat contains
  the id of every element that should be played at its index."
  [beats]
  (map beat-as-element-ids beats))

(defn provision-stop-steps
  "Provides a quantized sequence (in q-steps) of normalized beats where each step beat contains
  the id of every element that should be stopped at its index."
  [beats]
  (let [duration (count beats)
        items (mapcat index-beat-items beats)
        steps (-> duration (take (repeat '())) vec)]
    (reduce
     (fn [acc item]
       (let [index (cyclic-index duration (+ (:index item) (:duration item)))
             item-elems (many (element-as-ids (:elements item)))
             acc-elems (many (get acc index))
             elems (sort (into item-elems acc-elems))]
         (assoc acc index (distinct elems)))) steps items)))

(defn provision-steps
  "Provisions quantized :beat, :play and :stop sequences that describe what
  data is relevant on each step.
  Although dense, these sequences enable state-agnostic, context-free and
  efficient playback by preemptively calculating the state of each quantized step."
  [tree unit]
  (let [beats (normalize-beats tree unit)
        beat-steps (provision-context-steps tree unit)
        play-steps (provision-play-steps beats)
        stop-steps (provision-stop-steps beats)]
    (map (partial conj []) beat-steps play-steps stop-steps)))

(defn provision-elements
  "Groups all normalized beats' elements and their values by `kind`.
  Allows consumers to directly resolve elements keyed by their uid."
  [beats]
  (->>
   beats
   collect
   beat-as-elements
   (reduce
    (fn [acc element]
      (let [uid (element->uid element)
            kind (element->kind element)
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
  (map provision-beat (collect beats)))

(defn provision-units
  "Provisions essential and common unit values of a track for serialization and playback."
  [track]
  (let [beat-units {:step (tracks/get-step-beat track)
                    :pulse (tracks/get-pulse-beat track)}
        bar-units  {:step (tracks/get-step-beats-per-bar track)
                    :pulse (tracks/get-pulse-beats-per-bar track)}
        time-units {:step (tracks/get-step-beat-time track)
                    :pulse (tracks/get-pulse-beat-time track)}]
    {:beat beat-units
     :bar  bar-units
     :time (assoc time-units :bar
                  (* (time-units :step) (bar-units :step)))}))

(defn provision-metrics
  "Provisions basic metric values of step beats in a track that are useful for playback."
  [beats]
  (let [steps (map as-reduced-durations (mapcat :items beats))
        durations (collect steps)]
    {:min (apply min durations)
     :max (apply max durations)
     :total (count beats)}))

(defn provision-headers
  "Provisions essential and user-provided headers of a track for serialization and playback."
  [track]
  (let [headers (tracks/get-headers track)
        meter (tracks/get-meter track)]
    (assoc headers :meter meter)))

; TODO: Use keyword args to allow custom flexibile provisioning
;  - Consider setting flags, which would be used to control what gets provisioned and to inform engine so it can adapt its interpretation.
;  - Consider accept map of variables to allow composition against custom domains
(defn provision
  "Fully provisions a parsed track AST for high-level interpretation and playback."
  [data]
  (let [tree (tracks/parse data)
        track (tracks/playable identity tree)
        unit (unit-beat tree)
        beats (normalize-beats track unit)]
    {:iterations (tracks/get-iterations tree)
     :headers (provision-headers tree)
     :units (provision-units tree)
     :metrics (provision-metrics beats)
     :elements (provision-elements beats)
     :steps (provision-steps track unit)
     :beats (provision-beats beats)}))

(defn ^:export compose
  "Creates a normalized playable track from either a parsed AST or a UTF-8 string of bach data.
   Playable tracks are formatted so that they are easily and efficiently iterated over by a
  high-level bach engine (such as gig, for JS)."
  [data]
  (let [track (tracks/consume data)]
    (serialize
     (if (ast/parsed? track)
       (provision track)
       (into {:fail true} track)))))
