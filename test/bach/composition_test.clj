(ns bach.composition-test
  (:require [clojure.test :refer :all]
            [bach.track :refer :all]))

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
          want {:headers
                {:ms-per-beat-unit 500.0,
                 :pulse-beat 1/4,
                 :ms-per-pulse-beat 500.0,
                 :total-beats 1N,
                 :total-pulse-beats 4N,
                 :beat-unit 1/4,
                 :total-beat-units 4N,
                 :meter [4 4],
                 :tempo 120},
                :data
                [[{:duration 1, :notes [{:keyword "Chord", :arguments ["D2min7"]}]}
                  {:duration 2, :notes [{:keyword "Chord", :arguments ["G2Maj7"]}]}
                  nil
                  {:duration 1, :notes [{:keyword "Chord", :arguments ["C2maj7"]}]}]]}]
      (is (= want (compose tree)))))
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
                          :pulse-beat 3/4,
                          :ms-per-pulse-beat 3000.0,
                          :total-beats 3N,
                          :total-pulse-beats 4N,
                          :beat-unit 1/8,
                          :total-beat-units 24N,
                          :meter [6 8],
                          :tempo 120},
                :data [[{:duration 1, :notes [{:keyword "Chord", :arguments ["F#m"]}]}]
                       [{:duration 1, :notes [{:keyword "Chord", :arguments ["E"]}]}]
                       [{:duration 2, :notes [{:keyword "Chord", :arguments ["D"]}]}]
                       [nil]]}]
      (is (= want (compose tree))))))

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
                          :pulse-beat 3/4,
                          :ms-per-pulse-beat 1800.0,
                          :total-beats 3N,
                          :total-pulse-beats 4N,
                          :beat-unit 1/4,
                          :total-beat-units 12N,
                          :meter [3 4],
                          :tempo 100},
                :data [[{:duration 2,
                         :notes [{:keyword "Scale", :arguments ["C# phrygian"]}
                                 {:keyword "Chord", :arguments ["C#m"]}]}]
                       [nil]
                       [{:duration 2,
                         :notes [{:keyword "Chord", :arguments ["Dmaj7"]}]}]
                       [nil]]}]
      (is (= want (compose tree))))))
