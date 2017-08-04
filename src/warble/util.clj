(ns warble.util
  (:require [instaparse.core :as insta]))

(defn to-hashmap
  [tree]
  (insta/transform
    {:list (fn [& [:as all]] all)
     :atom (fn [& [:as all]] {:atom (apply merge all)})
     :header (fn [& [:as all]] {:header (apply merge all)})
     :meta (fn [el] {:meta el})
     :init (fn [el] {:init el})
     :arguments (fn [el] {:arguments el})
     :keyword (fn [el] {:keyword el})
     :play (fn [el] {:play el})}
   tree))


; FIXME: not quite - doesn't handle [:a [:b 1] [:c 2]] properly
; (defn hiccup-to-map
;   [tree]
;   (let [[k & v] tree]
;     (cond
;       (and (keyword? k) (vector? v))
;       {k (hiccup-to-map v)}

;       (and (keyword? k) (seq? v) (= 1 (count v)))
;       {k (hiccup-to-map v)}

;       :else
;       {k v})))
