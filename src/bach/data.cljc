(ns bach.data
  (:require #?@(:clj [[clojure.data.json :as json]]
                :cljs [;[bach.crypto]
                       [goog.crypt :as c]
                       [cljs.reader :as reader]])
            [instaparse.core :as insta]
            [nano-id.core :refer [custom]]))

(def from-string #?(:clj clojure.edn/read-string :cljs reader/read-string))
(def to-json #?(:clj json/write-str :cljs clj->js))

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

(defn expand
  [x n]
  (cons x (take (dec n) (repeat nil))))

(defn compare-items
  "Zipmaps source sequence of values (in) against comparison (values),
  specifying which elements of (in) are equal to those in (values)."
  [is? in values]
  (is? (zipmap in (repeat true)) (many values)))

(defn assoc-if
  ([coll kvs]
   (reduce #(assoc-if %1 (first %2) (last %2)) coll kvs))
  ([coll k v]
   (conj coll (when v [k v]))))

; clamp
(defn cyclic-index
  "Modulates index against a cycle limit, ensuring index is always in range."
  [limit index]
  (mod (if (>= index 0) index (+ limit index)) limit))

(defn nano-hash
  "Calculates a deterministic alphanumeric hash of any value using nano-id."
  [x]
  (let [alphabet "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        hash-gen (fn [n]
                   (->> x
                      hash
                      ; .toHashCode
                      (iterate #(unsigned-bit-shift-right % 6))
                      (take n)
                      reverse
                      ; #?(:cljs reverse)
                      #?(:clj byte-array)))
        hash-id (custom alphabet 6 hash-gen)]
  (hash-id)))

; TODO: Replace w/ slingshot
(defn problem
  "Throws a generic exception in a way that works in both Clojure and ClojureScript"
  [error]
  (throw #?(:clj (Exception. error)
            :cljs (js/Error. error))))
