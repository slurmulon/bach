(ns bach.composition-test
  (:require #?(:clj [clojure.test :refer :all]
               :cljs [cljs.test :refer-macros [deftest is testing run-tests]])
            ; [bach.track :refer [compose]]))
            [bach.track :as track]))



  ; #?(:clj (track/compose tree)
  ;    :cljs (-> tree track/compose js->clj)))

(defn sorted [data]
  ; (into (sorted-map) (sort-by first (seq data))))
  (into (sorted-map-by <) data))

(def hashed hash-unordered-coll)

(defn normalize [tree]
  (hashed tree))
  ; #?(:clj (hashed tree)
  ;    :cljs (-> tree js->clj hashed)))
  ; #?(:clj (hash-unordered-coll tree)
  ;    :cljs (-> tree js->clj hash-unordered-coll)))

  ; #?(:clj (sorted tree)
  ;    :cljs (-> tree sorted clj->js)))

  ; #?(:clj tree
  ;    :cljs (clj->js tree)))
     ; :clj (json/write-str tree)
     ; :cljs (-> tree js/JSON.stringify)))

; (defn compose [tree]
;   (-> tree
;       track/compose
;       ; #?(:cljs #(js->clj % :keywordize-keys true))
;       sorted))

(defn compose [tree]
  (hashed #?(:clj (-> tree track/compose sorted)
     :cljs (sorted (js->clj (track/compose tree) :keywordize-keys true)))))
  ; #?(:clj (-> tree track/compose sorted)
  ;    :cljs (sorted (js->clj (track/compose tree) :keywordize-keys true))))

(deftest basic
  (testing "common meter"
    (let [tree [:track
                [:statement
                 [:assign
                  [:identifier ":ABC"]
                  [:list
                   [:pair
                    [:div [:number "1"] [:number "4"]]
                    [:atom
                     [:keyword "Chord"]
                     [:arguments [:string "'D2min7'"]]]]
                   [:pair
                    [:div [:number "1"] [:number "2"]]
                    [:atom
                     [:keyword "Chord"]
                     [:arguments [:string "'G2Maj7'"]]]]
                   [:pair
                    [:div [:number "1"] [:number "4"]]
                    [:atom
                     [:keyword "Chord"]
                     [:arguments [:string "'C2maj7'"]]]]]]]
                [:statement
                 [:play [:identifier ":ABC"]]]]
          want {:headers {:ms-per-beat-unit 500.0,
                          :beat-units-per-measure 4,
                          :pulse-beat (/ 1 4),
                          :ms-per-pulse-beat 500.0,
                          :total-beats 1N,
                          :total-pulse-beats 4N,
                          :beat-unit (/ 1 4),
                          :total-beat-units 4N,
                          :pulse-beats-per-measure 4N,
                          :meter [4 4],
                          :tempo 120},
                :data [[{:duration 1,
                         :items [{:keyword "Chord", :arguments ["D2min7"]}]}
                        {:duration 2,
                         :items [{:keyword "Chord", :arguments ["G2Maj7"]}]}
                        nil
                        {:duration 1,
                         :items [{:keyword "Chord", :arguments ["C2maj7"]}]}]]}]
      ; (is (= want (compose tree)))))
      (is (= (normalize want) (compose tree)))))
      ; (is (= (hashed want) (normalize (compose tree))))))
  (testing "less common meter"
    (let [tree [:track
                [:statement
                 [:header
                  [:meta "Meter"]
                  [:meter [:number "6"] [:number "8"]]]]
                [:play
                 [:list
                  [:pair
                   [:mul
                    [:number "1"]
                    [:div [:number "6"] [:number "8"]]]
                   [:atom
                    [:keyword "Chord"]
                    [:arguments [:string "'F#m'"]]]]
                  [:pair
                   [:mul
                    [:number "1"]
                    [:div [:number "6"] [:number "8"]]]
                   [:atom
                    [:keyword "Chord"]
                    [:arguments [:string "'E'"]]]]
                  [:pair
                   [:mul
                    [:number "2"]
                    [:div [:number "6"] [:number "8"]]]
                   [:atom
                    [:keyword "Chord"]
                    [:arguments [:string "'D'"]]]]]]]
          want {:headers {:ms-per-beat-unit 500.0,
                          :beat-units-per-measure 6,
                          :pulse-beat (/ 3 4),
                          :ms-per-pulse-beat 3000.0,
                          :total-beats 3N,
                          :total-pulse-beats 4N,
                          :beat-unit (/ 1 8),
                          :total-beat-units 24N,
                          :pulse-beats-per-measure 1N,
                          :meter [6 8],
                          :tempo 120},
                :data [[{:duration 1,
                         :items [{:keyword "Chord", :arguments ["F#m"]}]}]
                       [{:duration 1,
                         :items [{:keyword "Chord", :arguments ["E"]}]}]
                       [{:duration 2,
                         :items [{:keyword "Chord", :arguments ["D"]}]}]
                       [nil]]}]
      ; (is (= want (compose tree))))))
      (is (= (normalize want) (compose tree))))))
      ; (is (= (hashed want) (normalize (compose tree)))))))
      ; (is (= #?(:clj want :cljs (js->clj want)) (compose tree))))))

(deftest advanced
  (testing "beat duration exceeds single measure"
    (let [tree [:track
                [:statement
                 [:header [:meta "Tempo"] [:number "100"]]]
                [:statement
                 [:header
                  [:meta "Meter"]
                  [:meter [:number "3"] [:number "4"]]]
                 [:play
                  [:list
                   [:pair
                    [:div [:number "6"] [:number "4"]]
                    [:set
                     [:atom
                      [:keyword "Scale"]
                      [:arguments [:string "'C# phrygian'"]]]
                     [:atom
                      [:keyword "Chord"]
                      [:arguments [:string "'C#m'"]]]]]
                   [:pair
                    [:div [:number "6"] [:number "4"]]
                    [:atom
                     [:keyword "Chord"]
                     [:arguments [:string "'Dmaj7'"]]]]]]]]
          want {:headers {:ms-per-beat-unit 600.0,
                          :beat-units-per-measure 3,
                          :pulse-beat (/ 3 4),
                          :ms-per-pulse-beat 1800.0,
                          :total-beats 3N,
                          :total-pulse-beats 4N,
                          :beat-unit (/ 1 4),
                          :total-beat-units 12N,
                          :pulse-beats-per-measure 1N,
                          :meter [3 4],
                          :tempo 100},
                :data [[{:duration 2,
                         :items [{:keyword "Scale", :arguments ["C# phrygian"]}
                                 {:keyword "Chord", :arguments ["C#m"]}]}]
                       [nil]
                       [{:duration 2,
                         :items [{:keyword "Chord", :arguments ["Dmaj7"]}]}]
                       [nil]]}]
      ; (is (= want (compose tree))))))
      (is (= (normalize want) (compose tree))))))
      ; (is (= (hashed want) (normalize (compose tree)))))))
