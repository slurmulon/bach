(ns warble.serialize
  (:require [clojure.data.json :as json]
            [warble.interpret :as interp]))

(defn to-json
  [track]
  (let [compiled-track (interp/compile-track track)]
    (json/write-str track)))
