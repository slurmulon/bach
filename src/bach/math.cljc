(ns bach.math)

(def to-ratio #?(:clj rationalize :cljs float))

(def powers-of-two (iterate (partial * 2) 1))

(defn gcd
  "Determines the greatest common denominator between two numeric values."
  [a b]
  (if (zero? b)
    a
    (recur b (mod a b))))

(defn ratio-to-vector
  "Converts a ratio to a vector."
  [ratio]
  #?(:clj
     (cond
       (ratio? ratio) [(numerator ratio) (denominator ratio)]
       (vector? ratio) ratio
       :else (throw (Exception. (str "Input must be a ratio or a vector, given: " ratio))))
     :cljs
     (cond
       (not (js/isNaN ratio)) [(* ratio 10) 10]
       (vector? ratio) ratio
       :else (throw (js/Error. (str "Input must be a number or a vector, given: " ratio))))))

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
