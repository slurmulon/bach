(ns bach.data
  (:require [instaparse.core :as insta]
            [clojure.data.json :as json]))

(def to-json json/write-str)

; TODO: make sure this returns an array when appropriate
; @see bach.track-test/compilation
(defn hiccup-to-hash-map
  "Converts an instaparse :hiccup tree as a hash map"
  [tree]
  (insta/transform
   {:list (fn [& [:as all]] all)
    :set (fn [& [:as all]] all)
    :atom (fn [& [:as all]] {:atom (apply merge all)})
    :arguments (fn [& [:as all]] {:arguments (vec all)})
    :header (fn [& [:as all]] {:header (apply merge all)})
    :meta (fn [el] {:meta el})
    :init (fn [el] {:init el})
    :keyword (fn [el] {:keyword el})
    :play (fn [el] {:play el})}
   tree))

(defn hiccup-to-json
  "Converts an instaparse :hiccup tree into JSON"
  [tree]
  (-> tree
      hiccup-to-hash-map
      to-json))

(defn ratio-to-vector
  "Converts a ratio to a vector"
  [ratio]
  (cond
    (ratio? ratio) [(numerator ratio) (denominator ratio)]
    (vector? ratio) ratio
    :else (throw (Exception. "input must be a ratio or a vector"))))

(defn inverse-ratio
  "Calculates the inverse of a ratio"
  [ratio]
  (if (integer? ratio)
    (/ 1 ratio)
    (let [[ratio-numerator & [ratio-denominator]] (ratio-to-vector ratio)]
      (/ ratio-denominator ratio-numerator))))

(defn trim-matrix-row
  "Trims columns from a specified row in a matrix (i.e nested array of depth 2)"
  [matrix row cols]
  (if (> cols 0)
    (let [clip #(->> % (drop-last cols) (into []))]
      (update matrix row clip))
    matrix))
