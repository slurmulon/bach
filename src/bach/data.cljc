(ns bach.data
  (:require [instaparse.core :as insta]
            [hiccup-find.core :refer [hiccup-find]]
            #?(:clj [clojure.data.json :as json]
               :cljs [cljs.reader :as reader])))

(def to-string #?(:clj clojure.edn/read-string :cljs reader/read-string))
(def to-json #?(:clj json/write-str :cljs clj->js))
(def to-ratio #?(:clj rationalize :cljs float))

(def math-floor #?(:clj #(Math/floor %) :cljs js/Math.floor))
(def math-ceil #?(:clj #(Math/ceil %) :cljs js/Math.ceil))

(def powers-of-two (iterate (partial * 2) 1))

(defn gcd
  "Determines the greatest common denominator between two numeric values."
  [a b]
  (if (zero? b)
    a
    (recur b (mod a b))))

; TODO: @see core/fractions (not necessary but probably more pragmatic)
(defn itemize
  "Provides a sized sequence where each element contains the provided value."
  [size value]
  (take size (repeat value)))

(defn many
  "Normalizes sequences, sets, maps and scalar values into a sequence."
  [x]
  (cond
    (or (sequential? x) (set? x)) (seq x)
    (nil? x) []
    :else (cons x [])))

(defn collect
  "Normalizes value as a sequence and filters out any nil elements."
  [x]
  (->> x many (filter (complement nil?))))

(defn compare-items
  "Zipmaps source sequence of values (in) against comparison (values),
  specifying which values of (in) are equal to those in (values)."
  [is? in values]
  (is? (zipmap in (repeat true)) (many values)))

(defn cyclic-index
  "Modulates index against a cycle limit, ensuring index is always in range."
  [limit index]
  (mod (if (>= index 0) index (+ limit index)) limit))

(defn hiccup-find-trees
  "Finds all of the child trees of hiccup nodes with a matching tag."
  [tags tree]
  (->> tree (hiccup-find tags) (map #(last %))))

(defn cast-tree
  "Walks an iterable N-ary tree (depth-first, pre-order) and applies `as` to each node where `is?` returns true."
  [is? as tree]
  (clojure.walk/prewalk #(if (is? %) (as %) %) tree))

(defn flatten-by
  "Flattens and reduces collection using `by` function."
  [by coll]
  (if (sequential? coll)
    (reduce by (flatten coll))
    (by coll)))

(defn flatten-one
  "Flattens nested collection by only one level."
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
  @see 'Notes': https://clojuredocs.org/clojure.core/flatten"
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

(defn squash
  "Same as flatten-tree but subsequently flattens all sets.
   Sets are assumed to only contain sets and other non-sequentials."
  [tree]
  (->> tree
       flatten-tree
       (cast-tree set? flatten-sets)))

(defn quantize
  "Provides a 1-ary stepwise projection of a weighted 1-ary coll, where
  each node's value represents its frequency/occurance (in other words, how
  many elements it takes up in the projected linear sequence).
  Input:  (stretch [1 4 3])
  Output: (0 1 1 1 1 2 2 2)"
  [coll]
  (->> coll (map-indexed #(itemize %2 %1)) flatten))

(defn transpose
  "Takes a collection of vectors and produces an inverted linear projection,
  where each element contains every element at the same index across each vector.
  @see: https://stackoverflow.com/a/29240104
  Input:  [[1 2] [3 4] [5 6] [7 8 9]]
  Output: [[1 3 5 7] [2 4 6 8] [9]]"
  [coll]
  (mapv (fn [index]
          (mapv #(get % index)
                (filter #(contains? % index) coll)))
        (->> (map count coll)
             (apply max)
             range)))

(defn linearize-indices
  "Projects the starting indices of each weighted node in coll/tree onto linear space.
  Enables consumers to easily index associated data, statefully or statelessly,
  by providing a linear, ordered (depth-first projection of a weighted N-ary tree.
  Input:  [2 3 4 7]
  Output: [0 2 5 9]"
  ([coll] (linearize-indices identity coll))
  ([xf coll]
   (->> coll
        drop-last
        xf
        (reduce (fn [acc, weight]
                  (let [base (last acc)
                        cursor (+ base weight)]
                    (conj acc cursor))) [0]))))

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
