(ns bach.data
  (:require [instaparse.core :as insta]
            #?(:clj [clojure.data.json :as json]
               :cljs [cljs.reader :as reader])))

(def to-string #?(:clj clojure.edn/read-string :cljs reader/read-string))
(def to-json #?(:clj json/write-str :cljs clj->js))
(def to-ratio #?(:clj rationalize :cljs float))

(def math-floor #?(:clj #(Math/floor %) :cljs js/Math.floor))
(def math-ceil #?(:clj #(Math/ceil %) :cljs js/Math.ceil))

(def powers-of-two (iterate (partial * 2) 1))

(defn gcd
  "Determines the greatest common denominator between two numeric values"
  [a b]
  (if (zero? b)
    a
    (recur b (mod a b))))

; TODO: @see core/fractions (not necessary but probably more pragmatic)
(defn itemize
  [size value]
  (take size (repeat value)))

(defn many
  "Normalizes all sequences, sets, maps and scalar values into a sequence."
  [x]
  (if (or (sequential? x) (set? x)) (seq x) (cons x [])))

(defn hiccup-to-hash-map
  "Converts an instaparse :hiccup tree as a hash map"
  [tree]
  (insta/transform
   {:list (fn [& [:as all]] all)
    :set (fn [& [:as all]] all)
    ; EXPERIMENT: Expanding loop at this level so that we don't have to return a new [:list ...] (or whatever) just to keep working with AST in certain `bach.compose` methods
    :loop (fn [iters & [:as all]] (->> all (map #(itemize iters) flatten vec)))
    :atom (fn [& [:as all]] {:atom (apply merge all)})
    :arguments (fn [& [:as all]] {:arguments (vec all)})
    :header (fn [& [:as all]] {:header (apply merge all)})
    :meta (fn [el] {:meta el})
    :init (fn [el] {:init el})
    ; TODO: Rename to `kind`
    :keyword (fn [el] {:keyword el})
    :play (fn [el] {:play el})}
   tree))

(defn hiccup-to-vector
  "Converts an instaparse :hiccup tree into a flat vector."
  [tree]
  (-> [tree]
      hiccup-to-hash-map
      flatten
      vec))

(defn hiccup-to-json
  "Converts an instaparse :hiccup tree into JSON."
  [tree]
  (-> tree
      hiccup-to-hash-map
      to-json))

(defn cast-tree
  "Walks an iterable N-ary tree (depth-first, pre-order) and applies `as` to each node where `is?` matches (`is?` returns true)"
  [is? as tree]
  (clojure.walk/prewalk #(if (is? %) (as %) %) tree))

(defn flatten-by
  "Flattens and reduces collection/tree using provided function."
  [by coll]
  (if (sequential? coll)
    (reduce by (flatten coll))
    (by coll)))

(defn flatten-one
  "Flattens nested collection or tree by only one level."
  [coll]
  (mapcat #(if (sequential? %) % [%]) coll))

(defn flatten-sets
  "Like flatten, but pulls elements out of sets instead of sequences.
  Does NOT support sequences nested in sets (only homogenous set trees).
  @see: https://gist.github.com/bdesham/1005837"
  [coll]
  (set (filter (complement set?)
               (rest (tree-seq set? seq (set coll))))))

(defn flatten-tree
  "Like `clojure.core/flatten` but better, stronger, faster.
  Takes any nested combination of sequential things (lists, vectors,
  etc.) and returns their contents as a single, flat, lazy sequence.
  If the argument is non-sequential (numbers, maps, strings, nil, 
  etc.), returns the original argument.
  @credit 'Notes': https://clojuredocs.org/clojure.core/flatten"
  {:static true}
  [tree]
  (letfn [(flat [coll]
            (lazy-seq
              (when-let [items (seq coll)]
                (let [node (first items)]
                  (if (sequential? node)
                    (concat (flat node) (flat (rest items)))
                    (cons node (flat (rest items))))))))]
    (if (sequential? tree) (flat tree) tree)))

(defn squash-tree
  "Same as flatten-tree, but also flattens all sets (TODO: Try to avoid doing this secondarily).
   Sets are assumed to be homogenous (can only contain sets and other non-sequentials)."
  [tree]
  (->> tree
       flatten-tree
       (cast-tree set? flatten-sets)))
; (squash-tree #{:a #{:b :c}})

(defn greatest-in
  [coll]
  (flatten-by max coll))

(defn linearize
  "Provides a 1-ary linear-space projection of the greatest weights among each root-level node and its children.
  Input:  [2 [3 1] [4 [5 7]]]
  Output: [2 3 7]"
  [coll]
  (map greatest-in coll))

(defn linearize-indices
  "Projects the starting indices of each weighted node in coll/tree onto linear space.
   Enables consumers to easily index associated data, statefully or statelessly, by providing a linear, ordered (depth-first projection of a weighted N-ary tree.
   Input:  (2 3 4 7)
   Output: (0 2 5 9)"
  ([coll] (linearize-indices linearize))
  ([xf coll]
   (->> coll
        drop-last
        xf
        (reduce (fn [acc, weight]
                  (let [base (last acc)
                        cursor (+ base weight)]
                    (conj acc cursor))) [0]))))

(defn stretch
  "Provides a 1-ary stepwise projection of a weighted N-ary coll/tree, where each node's value represents its frequency/occurance (in other words, how many elements it takes up in the projected linear sequence).
  Example:
   - Input:  (stretch [1 4 3])
   - Output: (0 1 1 1 1 2 2 2)"
  [coll]
  (->> coll (map-indexed #(itemize %2 %1)) flatten))

;; WORKS!
; TODO: Rename to quantize-durations
(defn quantize
  "Linearizes a weighted N-ary coll/tree, projecting each node's weight (i.e. how many elements it takes up) onto a 1-ary step-wise vector.
   This data structure is ideal for performing linear bi-directional indexing of non-linear collections."
  [coll]
  (->> coll linearize stretch vec))

(defn transpose
  "Takes a collection of vectors and produces an inverted linear project, where each element contains every element at the same index across each vector.
  @see: https://stackoverflow.com/a/29240104
  Input:  [[1 2] [3 4] [5 6] [7 8 9]]
  Output: [[1 3 5 7] [2 4 6 8] [9]]"
  [coll]
  (mapv (fn [ind]
          (mapv #(get % ind)
                (filter #(contains? % ind) coll)))
        (->> (map count coll)
             (apply max)
             range)))

(defn ratio-to-vector
  "Converts a ratio to a vector."
  [ratio]
  #?(:clj
    (cond
      (ratio? ratio) [(numerator ratio) (denominator ratio)]
      (vector? ratio) ratio
      :else (throw (Exception. "Input must be a ratio or a vector")))
    :cljs
    (cond
      (not (js/isNaN ratio)) [(* ratio 10) 10]
      (vector? ratio) ratio
      :else (throw (js/Error. "Input must be a number or a vector")))))

(defn inverse-ratio
  "Calculates the inverse of a ratio."
  [ratio]
  (if (integer? ratio)
    (/ 1 ratio)
    (let [[ratio-numerator & [ratio-denominator]] (ratio-to-vector ratio)]
      (/ ratio-denominator ratio-numerator))))

(defn safe-ratio
  "Divides two numeric values in a safe way that defaults to 0 during exceptions.
   Ideal when y might be 0 and you want to avoid explicitly handling this case."
  [x y]
  #?(:clj
     (try (/ x y)
          (catch ArithmeticException _
            0))
     :cljs
     (try (let [ratio (/ x y)]
            (case ratio
              (js/Infinity 0)
              (js/NaN 0)
              ratio))
          (catch js/Error _
            0))))

(defn trim-matrix-row
  "Trims tail columns from a specified row in a matrix (i.e nested array of depth 2)."
  [matrix row cols]
  (if (> cols 0)
    (let [clip #(->> % (drop-last cols) (into []))]
      (update matrix row clip))
    matrix))

(defn problem
  "Throws a generic exception in a way that works in both Clojure and ClojureScript"
  [error]
  (throw #?(:clj (Exception. error)
            :cljs (js/Error. error))))
