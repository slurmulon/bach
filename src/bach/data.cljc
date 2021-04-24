(ns bach.data
  (:require [instaparse.core :as insta]
            [hiccup-find.core :refer [hiccup-find]]
            #?(:clj [clojure.data.json :as json]
               :cljs [cljs.reader :as reader])))

(def to-string #?(:clj clojure.edn/read-string :cljs reader/read-string))
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

(defn compare-items
  "Zipmaps source sequence of values (in) against comparison (values),
  specifying which values of (in) are equal to those in (values)."
  [is? in values]
  (is? (zipmap in (repeat true)) (many values)))

(defn cyclic-index
  "Modulates index against a cycle limit, ensuring index is always in range."
  [limit index]
  (mod (if (>= index 0) index (+ limit index)) limit))

(defn problem
  "Throws a generic exception in a way that works in both Clojure and ClojureScript"
  [error]
  (throw #?(:clj (Exception. error)
            :cljs (js/Error. error))))
