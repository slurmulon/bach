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
          want {:headers {:meter [4 4],
                          :total-beats 1N,
                          :ms-per-pulse-beat 500.0,
                          :pulse-beat 1/4,
                          :tempo 120},
                :data [[{:duration 1/4,
                          :notes {:atom {:keyword "Chord",
                                        :arguments ["D2min7"]}}}
                        {:duration 1/2,
                          :notes {:atom {:keyword "Chord",
                                        :arguments ["G2Maj7"]}}}
                        nil
                        {:duration 1/4,
                          :notes {:atom {:keyword "Chord",
                                        :arguments ["C2maj7"]}}}]]}]
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
          want {:headers {:meter [6 8],
                          :total-beats 3N,
                          :ms-per-pulse-beat 1500.0,
                          :pulse-beat 3/4,
                          :beat-unit 1/8,
                          :tempo 120},
                :data [[{:duration 3/4,
                          :notes {:atom {:keyword "Chord",
                                        :arguments ["F#m"]}}}]
                        [{:duration 3/4,
                          :notes {:atom {:keyword "Chord",
                                        :arguments ["E"]}}}]
                        [{:duration 3/2,
                          :notes {:atom {:keyword "Chord",
                                        :arguments ["D"]}}}]
                        [nil]]}]
      (is (= want (compose tree))))))

(deftest advanced
  ; TODO: Consider moving to `normalization` suite
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
          want {:headers {:meter [3 4],
                          :total-beats 3N,
                          :ms-per-pulse-beat 1800.0,
                          :pulse-beat 3/4,
                          :tempo 100},
                :data [[{:duration 3/2,
                          :notes
                          [{:atom {:keyword "Scale",
                                  :arguments ["C# phrygian"]}}
                          {:atom {:keyword "Chord",
                                  :arguments ["C#m"]}}]}]
                        [nil]
                        [{:duration 3/2,
                          :notes {:atom {:keyword "Chord",
                                        :arguments ["Dmaj7"]}}}]
                        [nil]]}]
      (is (= want (compose tree))))))

; TODO: Move this to `normalize-measures`
; (deftest "obtuse meter (e.g. 5|8, 3|8)"
;   (let [tree [:track
;               [:statement [:header [:meta "Tempo"] [:number "75"]]]
;               [:statement
;                 [:header [:meta "Meter"] [:meter [:number "5"] [:number "8"]]]
;                 [:play
;                 [:list
;                   [:pair
;                   [:div [:number "3"] [:number "8"]]
;                   [:set
;                     [:atom
;                     [:keyword "Scale"]
;                     [:arguments [:string "'D dorian'"]]]
;                     [:atom
;                     [:keyword "Chord"]
;                     [:arguments [:string "'Dm9'"]]]]]
;                   [:pair
;                   [:div [:number "2"] [:number "8"]]
;                   [:atom
;                     [:keyword "Chord"]
;                     [:arguments [:string "'Am9'"]]]]]]]]
;         want {:headers
;               {:tags [],
;                 :desc "",
;                 :pulse-beat 1/8,
;                 :meter [5 8],
;                 :total-beats 5/8,
;                 :title "Untitled",
;                 :link "",
;                 :ms-per-pulse-beat 800.0,
;                 :audio "",
;                 :tempo 75,
;                 :ms-per-unit 800N},
;               :data
;               [[{:duration 3N,
;                   :notes
;                   ({:atom {:keyword "Scale", :arguments ["D dorian"]}}
;                   {:atom {:keyword "Chord", :arguments ["Dm9"]}})}
;                 nil
;                 nil
;                 {:duration 2N,
;                   :notes {:atom {:keyword "Chord", :arguments ["Am9"]}}}
;                 nil]]}]
;     (clojure.pprint/pprint (compose tree))
;     (is (= want (compose tree)))))
