(ns bach.core
  (:require [bach.track :as track]
            [cljs.nodejs :as nodejs]))

(def ^:export compose track/compose)

(defn noop [] nil)

(nodejs/enable-util-print!)

(set! *main-cli-fn* noop)
; (set! *main-cli-fn* track/compose)
