(ns bach.data
  (:require [instaparse.core :as insta]
            #?(:clj [clojure.data.json :as json]
               :cljs [cljs.reader :as reader])))
  ; (:refer-clojure :exclude [slurp]))

(def to-string #?(:clj clojure.edn/read-string :cljs reader/read-string))
(def to-json #?(:clj json/write-str :cljs clj->js))

(def math-floor #?(:clj #(Math/floor %) :cljs js/Math.floor))
(def math-ceil #?(:clj #(Math/ceil %) :cljs js/Math.ceil))

(defn gcd [a b]
  (if (zero? b)
    a
    (recur b (mod a b))))

(def powers-of-two (iterate (partial * 2) 1))

; (defmacro slurp [file]
;   (clojure.core/slurp file))

; TODO: May need to enhance how we work with slurp here
;  - Compiler warns but oddly we can access and use bach/ast from the cljsbuild repl
; @see: https://gist.github.com/noprompt/9086232
(defmacro inline-resource [resource-path]
  ; (slurp (clojure.java.io/resource resource-path)))
  (clojure.core/slurp (clojure.java.io/resource resource-path)))

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

(defn ratio-to-vector
  "Converts a ratio to a vector."
  [ratio]
  #?(:clj
    (cond
      (ratio? ratio) [(numerator ratio) (denominator ratio)]
      (vector? ratio) ratio
      :else (throw (Exception. "input must be a ratio or a vector")))
    :cljs
    (cond
      (not (js/isNaN ratio)) [(* ratio 10) 10]
      (vector? ratio) ratio
      :else (throw (js/Error. "input must be a number or a vector")))))

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
