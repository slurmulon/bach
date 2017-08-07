(ns warble.translate
  (:require [instaparse.core :as insta]
            [clojure.data.json :as json]))

(def to-json json/write-str)

; TODO: make sure this returns an array when appropriate
; @see warble.track-test/compilation
(defn hiccup-to-hash-map
  [tree]
  (insta/transform
    {:list (fn [& [:as all]] all)
     :atom (fn [& [:as all]] {:atom (apply merge all)})
     :arguments (fn [& [:as all]] {:arguments (vec all)})
     :header (fn [& [:as all]] {:header (apply merge all)}) ; TODO: try out (apply cons all) instead
     :meta (fn [el] {:meta el})
     :init (fn [el] {:init el})
     :keyword (fn [el] {:keyword el})
     :play (fn [el] {:play el})}
   tree))

(defn hiccup-to-json
  [tree]
  (-> tree
      hiccup-to-hash-map
      to-json))
