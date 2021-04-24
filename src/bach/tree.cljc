(ns bach.tree
  (:require [hiccup-find.core :refer [hiccup-find]]))

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

; TODO: @see core/fractions (not necessary but probably more pragmatic)
(defn itemize
  "Provides a sized sequence where each element contains the provided value."
  [size value]
  (take size (repeat value)))

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

(defn hiccup-query
  "Finds all of the child trees of hiccup nodes with a matching tag."
  [tags tree]
  (->> tree (hiccup-find tags) (map #(last %))))
