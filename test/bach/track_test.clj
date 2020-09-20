(ns bach.track-test
  (:require [clojure.test :refer :all]
            [bach.track :refer :all]))

(deftest defaults
  (testing "tempo"
    (is (= 120 default-tempo)))
  (testing "time-signature"
    (is (= [4 4] default-time-signature)))
  (testing "scale"
    (is (= "C2 Major" default-scale))))

; TODO: change the values from strings to keywords, naturally supported in clojure. derp.
(deftest validatation
  (testing "assignment"
    (let [tree [:track [:statement [:assign [:identifier ":Test"] [:number "1"]]]]]
      (is (= (validate tree) true))))
  (testing "identifier (valid, known variable)"
    (let [tree [:track [:statement [:assign [:identifier ":A"] [:number "1"]]]
                       [:statement [:assign [:identifier ":B"] [:identifier ":A"]]]]]
      (is (= (validate tree) true))))
  (testing "identifier (invalid, unknown variable)"
    (let [tree [:track [:statement [:assign [:identifier ":A"] [:number "1"]]
                                   [:assign [:identifier "B"] [:identifier "Z"]]]]]
      (is (thrown-with-msg? Exception #"variable is not declared before it's used" (validate tree)))))
  (testing "basic div (valid numerator and denominator)"
    (let [tree [:track [:statement [:div [:number "1"] [:number "2"]]]]]
      (is (= (validate tree) true))))
  (testing "basic div (valid numerator, invalid denominator)"
    (let [tree [:track [:statement [:div [:number "1"] [:number "3"]]]]]
      (is (thrown-with-msg? Exception #"note divisors must be base 2 and no greater than 512" (validate tree))))))
  ; NOTE: No longer seems to be a necessary requirement due to updated beat calculations (whole note)
  ; (testing "basic div (invalid numerator, valid denominator)"
  ;   (let [tree [:track [:statement [:div [:number "5"] [:number "4"]]]]]
  ;     (is (thrown-with-msg? Exception #"numerator cannot be greater than denominator" (validate tree))))))
  ; (testing "keyword"
  ;   (let [tree [:track [:statement [:keyword "Scale"] [:init [:arguments [:string [:word "C2 Major")

(deftest reduction
  (testing "number"
    (let [tree [:track [:statement [:div [:number "1"] [:number "4"]]]]
          want [:track [:statement 1/4]]]
      (is (= want (reduce-values tree)))))
  (testing "string"
    (let [tree [:track [:statement [:string "'Text'"]]]
          want [:track [:statement "Text"]]]
      (is (= want (reduce-values tree)))))
  (testing "operation"
    (let [tree [:track [:statement [:add [:div [:number "1"] [:number "2"]]
                                         [:div [:number "1"] [:number "4"]]]]]
          want [:track [:statement 3/4]]]
      (is (= want (reduce-values tree))))))

(deftest beat-unit
  (testing "basic"
    (let [tree [:track [:statement [:header [:meta "Time"] [:meter [:number "4"] [:number "4"]]]]]
          want 1/4]
      (is (= want (get-beat-unit tree)))))
  (testing "scaled"
    (testing "4/4"
      (let [tree [:track [:statement [:header [:meta "Time"]
                                              [:meter [:number "4"]
                                                      [:number "4"]]]]]
            want 1]
        (is (= want (get-scaled-beat-unit tree)))))
    (testing "6/8"
      (let [tree [:track [:statement [:header [:meta "Time"]
                                              [:meter [:number "6"]
                                                      [:number "8"]]]]]
            want 1/2]
        (is (= want (get-scaled-beat-unit tree)))))))

; FIXME: handle notes that aren't % 2
; FIXME: Write a test for when the lowest beat is not a modulus of the time signature
;  - e.g. "2 -> Chord('...')" in 6|8
(deftest lowest-beat
  (testing "whole number"
    (let [tree [:track [:statement [:list [:pair [:number "4"] [:list]]
                                          [:pair [:number "2"] [:list]]
                                          [:pair [:number "1"] [:list]]]]]
          want 1]
      (is (= want (get-lowest-beat tree)))))
  (testing "ratio"
    (let [tree [:track [:statement [:list [:pair [:div [:number "1"] [:number "2"]] [:list]]
                                          [:pair [:div [:number "1"] [:number "4"]] [:list]]
                                          [:pair [:div [:number "1"] [:number "8"]] [:list]]]]]
          want 1/8]
      (is (= want (get-lowest-beat tree)))))
  (testing "complex ratio"
    (let [tree [:track [:statement [:header [:meta "Time"]
                                            [:meter [:number "3"]
                                                    [:number "4"]]]]
                       [:statement [:list [:pair [:add [:div [:number "1"]
                                                             [:number "2"]]
                                                       [:div [:number "1"]
                                                             [:number "4"]]]
                                                 [:atom [:keyword "Note"]
                                                        [:init [:arguments [:string "'C2'"]]]]]]]]
          ; FIXME: Ideal value, but not entirely necessary
          want 3/4]
          ; want (/ 1 4)]
      (is (= want (get-lowest-beat tree)))))
  (testing "spanning multiple measures"
    (testing "aligned"
      (let [tree [:track [:statement [:header [:meta "Time"]
                                              [:meter [:number "6"]
                                                      [:number "8"]]]]
                        [:statement [:list [:pair [:mul [:number "2"]
                                                        [:div [:number "6"]
                                                              [:number "8"]]]
                                                  [:atom [:keyword "Note"]
                                                         [:init [:arguments [:string "'C2'"]]]]]]]]
            ; TODO: Eventually, once get-lowest-beat can support multiple measures via ##Inf (Clojure 1.9.946+)
            ; - Actually, probably abandoning this since things are easier if lowest-beat cannot exceed a measure (e.g. 1)
            ; want (/ 3 2)
            want 3/4]
            ; want (/ 1 2)]
        (is (= want (get-lowest-beat tree)))))
    (testing "misaligned"
      (let [tree [:track [:statement [:header [:meta "Time"]
                                              [:meter [:number "6"]
                                                      [:number "8"]]]]
                         [:statement [:list [:pair [:number "2"]
                                                   [:atom [:keyword "Note"]
                                                          [:init [:arguments [:string "'C2'"]]]]]]]]
            want 3/4]
        (is (= want (get-lowest-beat tree)))))
    (testing "aligned (alt)"
      (let [tree [:track [:statement [:header [:meta "Time"]
                                              [:meter [:number "3"]
                                                      [:number "4"]]]]
                         [:statement [:list [:pair [:div [:number "6"]
                                                         [:number "4"]]
                                                   [:atom [:keyword "Note"]
                                                          [:init [:arguments [:string "'C2'"]]]]]]]]
            ; LAST
            ; - Only want if we support lowest-common-beat exceeding an entire measure (seems too complicated right now)
            ; want (/ 6 4)]
            want 3/4]
        (is (= want (get-lowest-beat tree)))))))

(deftest total-beats
  (testing "common time"
    (let [tree [:track [:statement [:list [:pair [:number "1"] [:list]]
                                          [:pair [:number "4"] [:list]]
                                          [:pair [:div [:number "1"] [:number "4"]] [:list]]
                                          [:pair [:div [:number "1"] [:number "4"]] [:list]]]]]
          want (rationalize 5.5)]
      (is (= want (get-total-beats tree)))))
  (testing "less common time"
    (let [tree [:track [:statement [:meta "Time"]
                                   [:meter [:number "6"]
                                           [:number "8"]]]
                       [:statement [:list [:pair [:number "1"] [:list]]
                                          [:pair [:div [:number "1"] [:number "8"]] [:list]]]]]
          want 9/8]
      (is (= want (get-total-beats tree))))))

; TODO: Replace with normalized-total-measures, this is redundant since get-total-beats is the same here
; (deftest total-measures)
;   (testing "common time"
;     (let [tree [:track [:statement [:list [:pair [:number "1"] [:list]]
;                                           [:pair [:number "4"] [:list]]
;                                           [:pair [:div [:number "1"] [:number "4"]] [:list]]
;                                           [:pair [:div [:number "1"] [:number "4"]] [:list]]]]]
;           want (rationalize 5.5)]
;       (is (= want (get-total-measures tree)))))

(deftest normalized-total-measures
  (testing "common time"
    (testing "beat unit matches lowest common beat"
      (let [tree [:track [:statement [:list [:pair [:number "1"] [:list]]
                                            [:pair [:number "4"] [:list]]
                                            [:pair [:div [:number "1"] [:number "4"]] [:list]]
                                            [:pair [:div [:number "1"] [:number "4"]] [:list]]]]]
            want (rationalize 5.5)]
        (is (= want (get-total-measures tree)))))
    (testing "beat unit is less (shorter) than lowest common beat"
      (let [tree [:track [:statement [:list [:pair [:div [:number "1"] [:number "2"]] [:list]]
                                            [:pair [:div [:number "1"] [:number "2"]] [:list]]]]]
            want 1]
        (is (= want (get-normalized-total-measures tree)))))
    (testing "beat unit is greater (longer) than lowest common beat"
      (let [tree [:track [:statement [:list [:pair [:div [:number "1"] [:number "8"]] [:list]]
                                            [:pair [:div [:number "1"] [:number "8"]] [:list]]
                                            [:pair [:div [:number "1"] [:number "8"]] [:list]]
                                            [:pair [:div [:number "1"] [:number "8"]] [:list]]]]]
            want (rationalize 0.5)]
        (is (= want (get-normalized-total-measures tree)))))
    (testing "total measures is less than duration of full bar"
      (let [tree [:track [:statement [:list [:pair [:div [:number "1"] [:number "8"]] [:list]]
                                            [:pair [:div [:number "1"] [:number "8"]] [:list]]]]]
            want (rationalize 0.25)]
        (is (= want (get-normalized-total-measures tree)))))
  (testing "less common time"
    (testing "beat unit matches lowest common beat"
      (let [tree [:track [:statement [:header [:meta "Time"]
                                              [:meter [:number "3"]
                                                      [:number "4"]]]]
                         [:statement [:list [:pair [:div [:number "3"] [:number "4"]] [:list]]
                                            [:pair [:div [:number "1"] [:number "4"]] [:list]]
                                            [:pair [:div [:number "1"] [:number "4"]] [:list]]
                                            [:pair [:div [:number "1"] [:number "4"]] [:list]]]]]
            want 2]
            ; want (rationalize 1.5)]
        (is (= want (get-normalized-total-measures tree)))))
    (testing "beat unit is less (shorter) than lowest common beat"
      (let [tree [:track [:statement [:header [:meta "Time"]
                                              [:meter [:number "3"]
                                                      [:number "4"]]]]
                         [:statement [:list [:pair [:div [:number "1"] [:number "8"]] [:list]]
                                            [:pair [:div [:number "1"] [:number "8"]] [:list]]
                                            [:pair [:div [:number "1"] [:number "4"]] [:list]]
                                            [:pair [:div [:number "1"] [:number "4"]] [:list]]]]]
            want 1]
        (is (= want (get-normalized-total-measures tree)))))
    ; TODO
    (testing "beat unit is greater (longer) than lowest common beat"
      (let [tree [:track [:statement [:header [:meta "Time"]
                                              [:meter [:number "3"]
                                                      [:number "4"]]]]
                         [:statement [:list [:pair [:div [:number "1"] [:number "8"]] [:list]]
                                            [:pair [:div [:number "1"] [:number "8"]] [:list]]
                                            [:pair [:div [:number "1"] [:number "8"]] [:list]]
                                            [:pair [:div [:number "1"] [:number "8"]] [:list]]
                                            [:pair [:div [:number "1"] [:number "8"]] [:list]]
                                            [:pair [:div [:number "1"] [:number "8"]] [:list]]]]]
            want 1]
        (is (= want (get-normalized-total-measures tree)))))
    (testing "total measures is less than duration of full bar"
      (let [tree [:track [:statement [:header [:meta "Time"]
                                            [:meter [:number "3"]
                                                    [:number "4"]]]]
                          [:statement [:list [:pair [:div [:number "1"] [:number "8"]] [:list]]
                                            [:pair [:div [:number "1"] [:number "8"]] [:list]]]]]
            want 1/3]
        (is (= want (get-normalized-total-measures tree)))))
)))

; (deftest normalized-total-measures
;   (testing "common time"
;     (testing "beat unit matches lowest common beat"

; (deftest get-normalized-beats-per-measure)

; TODO: Test, then refactor bach/track to use throughout
(deftest normalized-duration
  (testing "common times"
    (testing "beat unit matches lowest common beat"
      (let [duration 1/2
            lowest-beat 1/4
            time-sig 1
            want 2]
        (is (= want (normalize-duration duration lowest-beat time-sig)))))
    (testing "beat unit is less (shorter) than lowest common beat"
      (let [duration 1
            lowest-beat 1
            time-sig 2/4
            want 2]
        (is (= want (normalize-duration duration lowest-beat time-sig))))))
    (testing "beat unit is greater (longer) than lowest common beat"
      (let [duration 1/2
            lowest-beat 1/8
            time-sig 1
            want 4]
        (is (= want (normalize-duration duration lowest-beat time-sig)))))
    (testing "beat unit is greater (longer) than lowest common beat (2|4 time)"
      (let [duration 1/2
            lowest-beat 1/8
            time-sig 2/4
            want 1]
        (is (= want (normalize-duration duration lowest-beat time-sig)))))
    (testing "less common times"
      (testing "beat unit matches lowest common beat"
        (testing "duration matches full bar"
          (let [duration 5/8
                lowest-beat 1/8
                time-sig 5/8
                want 5]
            (is (= want (normalize-duration duration lowest-beat time-sig)))))
        (testing "duration is less than full bar (even meter)"
          (let [duration 4/8
                lowest-beat 1/8
                time-sig 6/8
                want 4]
            (is (= want (normalize-duration duration lowest-beat time-sig)))))
        (testing "duration is less than full bar (odd meter)"
          (let [duration 3/8
                lowest-beat 1/8
                time-sig 5/8
                want 3]
            (is (= want (normalize-duration duration lowest-beat time-sig)))))
        (testing "duration is greater than full bar and lowest-beat equals a full bar"
          (let [duration 6/4
                lowest-beat 3/4
                time-sig 3/4
                want 2]
            (is (= want (normalize-duration duration lowest-beat time-sig)))))
        (testing "duration is less than lowest-beat (edge case)"
          (let [duration 1/16
                lowest-beat 1/8
                time-sig 6/8
                want 1/2]
            (is (= want (normalize-duration duration lowest-beat time-sig))))))))

(deftest duration
  (testing "minutes"
    (testing "no extra seconds"
      (let [tree [:track [:statement [:list [:pair [:number "30"] [:list]]]]] ; 30 measures x 4 beats = 120 beats
            want 1]
        (is (= want (get-total-duration tree :minutes)))))
    (testing "with extra seconds"
      (let [tree [:track [:statement [:list [:pair [:number "20"] [:list]]
                                            [:pair [:number "25"] [:list]]]]] ; 20 measures x 4 beats = 80, 25 measures x 4 = 100 beats === 180 total beats
            want (rationalize 1.5)]
        (is (= want (get-total-duration tree :minutes))))))
  (testing "milliseconds"
    (let [tree [:track [:statement [:list [:pair [:number "30"] [:list]]]]] ; 30 measures x 4 beats = 120 beats
          want 60000]
      (is (= want (get-total-duration tree :milliseconds))))))

; (deftest normalize-duration
;   (testing "common time"
;     (testing "")))

; FIXME: if the duration of the beats is less than a measure, it ends up breaking
; the get-ms-per-beat calculation (stemming from total-duration-ms). Need a test for this!
(deftest milliseconds-per-beat
  (testing "singleton"
    (testing "whole note"
      (let [tree [:track [:statement [:list [:pair [:number "1"]
                                                   [:atom [:keyword "Note"]
                                                          [:init [:arguments [:string "'C2'"]]]]]]]]
            want 2000.0]
        (is (= want (get-ms-per-beat tree)))))
    (testing "half note"
      (let [tree [:track [:statement [:list [:pair [:div [:number "1"] [:number "2"]]
                                                   [:atom [:keyword "Note"] [:init [:arguments [:string "'C2'"]]]]]
                                            [:pair [:div [:number "1"] [:number "2"]]
                                                   [:atom [:keyword "Note"] [:init [:arguments [:string "'C2'"]]]]]]]]
            want 1000.0]
        (is (= want (get-ms-per-beat tree)))))
    (testing "quarter note"
      (let [tree [:track [:statement [:list [:pair [:div [:number "1"] [:number "4"]]
                                                   [:atom [:keyword "Note"] [:init [:arguments [:string "'C2'"]]]]]]]]
            want 500.0]
        (is (= want (get-ms-per-beat tree)))))
    (testing "eigth note"
      (let [tree [:track [:statement [:list [:pair [:div [:number "1"] [:number "8"]]
                                                   [:atom [:keyword "Note"] [:init [:arguments [:string "'C2'"]]]]]]]]
            want 250.0]
        (is (= want (get-ms-per-beat tree))))))
  ; FIXME: Could improve this by having `get-lowest-beat` look at the reduced values, right now it is using 1/4 when it could use 3/4 if it added 1/2 and 1/4 together first (this should already be happening, actually)
  (testing "non-default time signature"
    (testing "3/4"
      (let [tree [:track [:statement [:header [:meta "Time"]
                                              [:meter [:number "3"]
                                                      [:number "4"]]]]
                         [:statement [:list [:pair [:add [:div [:number "1"]
                                                               [:number "2"]]
                                                         [:div [:number "1"]
                                                               [:number "4"]]]
                                                   [:atom [:keyword "Note"]
                                                          [:init [:arguments [:string "'C2'"]]]]]]]]
                         ; [:statement [:list [:pair [:number "1"]
                         ;                           [:atom [:keyword "Note"]
                         ;                                  [:init [:arguments [:string "'C2'"]]]]]]]]
            ; FIXME: Use this value once get-lowest-beat is optimized and can return things like 3/4
            want 1500.0]
            ; want 500.0]
        (is (= want (get-ms-per-beat tree)))))
    (testing "6/8"
      (let [tree [:track [:statement [:header [:meta "Time"]
                                              [:meter [:number "6"]
                                                      [:number "8"]]]]
                         [:statement [:list [:pair [:number "1"]
                                                   [:atom [:keyword "Note"]
                                                          [:init [:arguments [:string "'C2'"]]]]]]]]
            ; LAST
            ; want 1500.0]
            ; want 2000.0]
            want 250.0] ; because "1", or 4 beats, does not align flushly with the meter
        (is (= want (get-ms-per-beat tree)))))))

; (deftest lowest-beat
;   (testing "finds the lowest beat amongst all of the pairs"
;     (let [tree [:track [:statement [:list [:pair [:number "4"]] [:pair [:number "2"]
;     )

; FIXME: nested transitive variables are broken (:A = 1, :B = :A, :C = :B)
(deftest dereference
  (testing "variables (simple)"
    (let [tree [:track [:statement [:assign [:identifier :A] [:number 1]]]
                       [:statement [:assign [:identifier :B] [:identifier :A]]]]
          want [:track [:statement [:assign [:identifier :A] [:number 1]]]
                       [:statement [:assign [:identifier :B] [:number 1]]]]]
      (is (= want (deref-variables tree)))))
  (testing "variables (nested)"
    (let [tree [:track [:statement [:assign [:identifier :A] [:number 1]]]
                       [:statement [:assign [:identifier :B] [:identifier :A]]]
                       [:statement [:assign [:identifier :C] [:identifier :B]]]]
          want [:track [:statement [:assign [:identifier :A] [:number 1]]]
                       [:statement [:assign [:identifier :B] [:number 1]]]
                       [:statement [:assign [:identifier :C] [:number 1]]]]]
      (is (= want (deref-variables tree)))))
  (testing "variables (list/array)"
    (let [tree [:track [:statement [:assign [:identifier :A] [:list [:number 1] [:number 2]]]]
                       [:statement [:assign [:identifier :B] [:identifier :A]]]]
          want [:track [:statement [:assign [:identifier :A] [:list [:number 1] [:number 2]]]]
                       [:statement [:assign [:identifier :B] [:list [:number 1] [:number 2]]]]]]
      (is (= want (deref-variables tree)))))
  (testing "variables (nested list/array)"
    (let [tree [:track [:statement [:assign [:identifier :A]
                                            [:atom [:keyword "Chord"]
                                                   [:init [:arguments [:string "'D2min7'"]]]]]]
                       [:statement [:assign [:identifier :B]
                                            [:list [:pair [:number "1"]
                                                          [:identifier :A]]]]]]
          want [:track [:statement [:assign [:identifier :A]
                                            [:atom [:keyword "Chord"]
                                                   [:init [:arguments [:string "'D2min7'"]]]]]]
                       [:statement [:assign [:identifier :B]
                                            [:list [:pair [:number "1"]
                                                          [:atom [:keyword "Chord"]
                                                                 [:init [:arguments [:string "'D2min7'"]]]]]]]]]]
      (is (= want (deref-variables tree)))))
  ; (testing "beats"
  ;   (let [tree [:track [:statement [:assign [:identifier ":ABC"] [:list [:pair [:number "1"] [:atom [:keyword "Chord"] [:init [:arguments [:string "'D2min7'"]]]]] [:pair [:number "3"] [:atom [:keyword "Chord"] [:init [:arguments [:string "'B2Maj7'"]]]]]]]]]
  ;         want [:track [:statement [:assign [:identifier ":ABC"] [:list [:beat [:list [:atom [:keyword "Chord"] [:init [:arguments [:string "'D2min7'"]]]]]] [:beat []] [:beat [:atom [:keyword "Chord"] [:init [:arguments [:string "'B2Maj7'"]]]]]]]]]]
  ;     (is (= true true))))) ; FIXME/TODO
)

(deftest scaled
  (testing "beats per measure"
    (testing "fully reduced ratio"
      (let [tree [:track [:statement [:header [:meta "Time"]
                                              [:meter [:number "3"]
                                                      [:number "4"]]]]]
            want 3]
        (is (= want (get-beats-per-measure tree)))))
    (testing "reduceable ratio (avoid reduction)"
      (let [tree [:track [:statement [:header [:meta "Time"]
                                              [:meter [:number "6"]
                                                      [:number "8"]]]]]
            want 6]
        (is (= want (get-beats-per-measure tree)))))))

; FIXME: might need to consider the surrounding :statement, :assign, :track, etc.
; - eh, maybe not
; TODO: also test with derefrenced variables, need to make sure it doesn't go the same measure value N times (where N is the number of references)
(deftest normalization
  (testing "total beats"
    (testing "using greater unit than denominator in time signature"
      (testing "single measure"
        (let [tree [:track [:statement [:assign [:identifier ":ABC"]
                                                [:list [:pair [:number "1"]
                                                              [:atom [:keyword "Chord"]
                                                                     [:init [:arguments [:string "'D2min7'"]]]]]]]]]
            want 4]
        (is (= want (get-normalized-total-beats tree)))))
      (testing "multiple measures"
        (let [tree [:track [:statement [:assign [:identifier ":ABC"]
                                                [:list [:pair [:number "1"]
                                                              [:atom [:keyword "Chord"]
                                                                      [:init [:arguments [:string "'D2min7'"]]]]]
                                                        [:pair [:number "1"]
                                                               [:atom [:keyword "Chord"]
                                                                      [:init [:arguments [:string "'G3maj7'"]]]]]]]]]
              want 8]
          (is (= want (get-normalized-total-beats tree)))))))
  ; TODO: "with greater than whole notes"
  (testing "measures"
    (testing "with whole notes"
      (let [tree [:track [:statement [:assign [:identifier ":ABC"]
                                              [:list [:pair [:number "1"]
                                                            [:atom [:keyword "Chord"]
                                                                   [:init [:arguments [:string "'D2min7'"]]]]]
                                                     [:pair [:number "1"]
                                                            [:atom [:keyword "Chord"]
                                                                   [:init [:arguments [:string "'G2Maj7'"]]]]]
                                                     [:pair [:number "1"]
                                                            [:atom [:keyword "Chord"]
                                                                   [:init [:arguments [:string "'C2maj7'"]]]]]]]]
                         [:statement [:play [:identifier ":ABC"]]]]
            want [[{:duration 1,
                    :notes {:atom {:keyword "Chord",
                                   :init {:arguments ["D2min7"]}}}}]
                  [{:duration 1,
                    :notes {:atom {:keyword "Chord",
                                   :init {:arguments ["G2Maj7"]}}}}]
                  [{:duration 1,
                    :notes {:atom {:keyword "Chord",
                                   :init {:arguments ["C2maj7"]}}}}]]]
        (is (= want (normalize-measures tree))))) )
    (testing "with half notes"
      (let [tree [:track [:statement [:assign [:identifier ":ABC"]
                                              [:list [:pair [:div [:number "1"]
                                                                  [:number "2"]]
                                                            [:atom [:keyword "Chord"]
                                                                   [:init [:arguments [:string "'D2min7'"]]]]]
                                                     [:pair [:div [:number "1"]
                                                                  [:number "2"]] 
                                                            [:atom [:keyword "Chord"]
                                                                   [:init [:arguments [:string "'G2Maj7'"]]]]]
                                                     [:pair [:number "1"]
                                                            [:atom [:keyword "Chord"]
                                                                   [:init [:arguments [:string "'C2maj7'"]]]]]]]]
                         [:statement [:play [:identifier ":ABC"]]]]
            want [[{:duration 1/2,
                    :notes {:atom {:keyword "Chord",
                                   :init {:arguments ["D2min7"]}}}}
                   {:duration 1/2,
                    :notes {:atom {:keyword "Chord",
                                   :init {:arguments ["G2Maj7"]}}}}]
                  [{:duration 1,
                    :notes {:atom {:keyword "Chord",
                                   :init {:arguments ["C2maj7"]}}}}
                   nil]]]
        (is (= want (normalize-measures tree)))))
    (testing "with quarter notes"
      (let [tree [:track [:statement [:assign [:identifier ":ABC"]
                                              [:list [:pair [:div [:number "1"]
                                                                  [:number "4"]]
                                                            [:atom [:keyword "Chord"]
                                                                   [:init [:arguments [:string "'D2min7'"]]]]]
                                                     [:pair [:div [:number "1"]
                                                                  [:number "2"]]
                                                            [:atom [:keyword "Chord"]
                                                                   [:init [:arguments [:string "'G2Maj7'"]]]]]
                                                     [:pair [:div [:number "1"]
                                                                  [:number "4"]]
                                                            [:atom [:keyword "Chord"]
                                                                   [:init [:arguments [:string "'C2maj7'"]]]]]]]]
                   [:statement [:play [:identifier ":ABC"]]]]
            want [[{:duration 1/4,
                    :notes {:atom {:keyword "Chord",
                                   :init {:arguments ["D2min7"]}}}}
                    {:duration 1/2,
                     :notes {:atom {:keyword "Chord",
                                    :init {:arguments ["G2Maj7"]}}}}
                    nil
                    {:duration 1/4,
                     :notes {:atom {:keyword "Chord",
                                    :init {:arguments ["C2maj7"]}}}}]]]
        (is (= want (normalize-measures tree)))))
      (testing "with eigth notes"
        (let [tree [:track [:statement [:play [:list [:pair [:div [:number "5"] [:number "8"]]
                                                            [:atom [:keyword "Chord"] [:init [:arguments [:string "Dmin7"]]]]]
                                                     [:pair [:div [:number "3"] [:number "8"]]
                                                            [:atom [:keyword "Chord"] [:init [:arguments [:string "Emin7"]]]]]]]]]
              want [[{:duration 5/8,
                      :notes {:atom {:keyword "Chord",
                                     :init {:arguments ["Dmin7"]}}}}
                       nil
                       nil
                       nil
                       nil
                       {:duration 3/8,
                        :notes {:atom {:keyword "Chord",
                                       :init {:arguments ["Emin7"]}}}}
                      nil
                      nil]]]
        (is (= want (normalize-measures tree)))))
      ; (testing "with odd number of beats per measure"
      ; TODO: We need to coerce the lowest beat to modulate with the number of beats in a measure
      ;  - For instance, if the meter is 5|8 and the lowest beat is 1/4, the lowest beat needs to become 1/8 instead
      ;  - Create a test for this in `get-lowest-beat` method
      ; (testing "with non-modular lowest beat"
      ;   (let [tree [:track [:statement [:header [:meta "Time"]
      ;                                           [:meter [:number "5"] [:number "8"]]]]
      ;                      [:statement [:play [:list [:pair [:div [:number "1"] [:number "4"]]
      ;                                                       [:atom [:keyword "Chord"]
      ;                                                              [:init [:arguments [:string "Dmin7"]]]]]]]]]
      ;         want [[:duration 1/4,
      ;                :notes {:atom {:keyword "Chord",
      ;                               :init {:arguments ["Dmin7"]}}}
      ;                nil
      ;                nil
      ;                nil
      ;                nil]]

  ; FIXME
  ; (testing "with nested measure references (should avoid dupes)"
  ;   (let [tree [:track [:statement [:assign [:identifier ":A"] [:list [:pair [:number "1"] [:atom [:keyword "Scale"] [:init [:arguments [:string "'C2 Major'"]]]]]]]] [:statement [:assign [:identifier ":B"] [:identifier ":A"]]] [:statement [:play [:identifier ":B"]]]]
  ;         want [[{:duration 1, :notes [:atom [:keyword "Scale"] [:init [:arguments [:string "C2 Major"]]]]}]]]
  ;     (is (= want (normalize-measures tree)))))
)

(deftest provisioning
  (testing "headers"
    (testing "static"
      ; (let [tree [:track [:statement [:header [:meta "Tempo"] [:number "90"]]]]
      ;       want 90]
      ;   (is (= (:tempo (provision-headers tree)) want)))
      (let [tree [:track [:statement [:header [:meta "Time"]
                                              [:meter [:number "3"] [:number "4"]]]]]
            want [3 4]]
        (is (= (:time (provision-headers tree)) want)))))
    (testing "dynamic"
      (testing "total beats"
        (let [tree [:track [:statement [:assign [:identifier ":ABC"]
                                                [:list [:pair [:number "1"] [:list]]
                                                       [:pair [:number "3"] [:list]]]]]]
              want 4]
          (is (= (:total-beats (provision-headers tree)) want)))))
      (testing "ms per beat"
        (let [tree [:track [:statement [:assign [:identifier ":ABC"]
                                                [:list [:pair [:number "1"] [:list]]
                                                       [:pair [:number "3"] [:list]]]]]]
              want 2000.0]
          (is (= (:ms-per-beat (provision-headers tree)) want))))
      (testing "lowest beat"
        (let [tree [:track [:statement [:assign [:identifier ":ABC"]
                                                [:list [:pair [:div [:number "1"]
                                                                    [:number "4"]]
                                                              [:list]]
                                                       [:pair [:number "1"]
                                                              [:list]]]]]]
              want 1/4]
          (is (= (:lowest-beat (provision-headers tree)) want)))))

; TODO: Probably just move this suite into its own file, semantically these are integration tests
(deftest compilation
  (testing "basic"
    (testing "common meter"
      (let [tree [:track [:statement [:assign [:identifier ":ABC"]
                                              [:list [:pair [:div [:number "1"] [:number "4"]]
                                                            [:atom [:keyword "Chord"]
                                                                  [:init [:arguments [:string "'D2min7'"]]]]]
                                                    [:pair [:div [:number "1"] [:number "2"]]
                                                            [:atom [:keyword "Chord"]
                                                                  [:init [:arguments [:string "'G2Maj7'"]]]]]
                                                    [:pair [:div [:number "1"] [:number "4"]]
                                                            [:atom [:keyword "Chord"]
                                                                  [:init [:arguments [:string "'C2maj7'"]]]]]]]]
                        [:statement [:play [:identifier ":ABC"]]]]
            want {:headers {:tags [],
                            :desc "",
                            :time [4 4],
                            :total-beats 1N,
                            :title "Untitled",
                            :link "",
                            :ms-per-beat 500.0,
                            :lowest-beat 1/4,
                            :audio "",
                            :tempo 120},
                  :data [[{:duration 1/4,
                          :notes {:atom {:keyword "Chord",
                                          :init {:arguments ["D2min7"]}}}}
                          {:duration 1/2,
                          :notes {:atom {:keyword "Chord",
                                          :init {:arguments ["G2Maj7"]}}}}
                          nil
                          {:duration 1/4,
                          :notes {:atom {:keyword "Chord",
                                          :init {:arguments ["C2maj7"]}}}}]]}]
        (is (= want (compile-track tree)))))
    (testing "non-common meter (8th note as unit)"
      (let [tree [:track [:statement [:header [:meta "Time"] [:meter [:number "6"] [:number "8"]]] [:play [:list [:pair [:mul [:number "1"] [:div [:number "6"] [:number "8"]]] [:atom [:keyword "Chord"] [:init [:arguments [:string "'F#m'"]]]]] [:pair [:mul [:number "1"] [:div [:number "6"] [:number "8"]]] [:atom [:keyword "Chord"] [:init [:arguments [:string "'E'"]]]]] [:pair [:mul [:number "2"] [:div [:number "6"] [:number "8"]]] [:atom [:keyword "Chord"] [:init [:arguments [:string "'D'"]]]]]]]]]
            want {:headers {:tags [], :desc "", :time [6 8], :total-beats 3N, :title "Untitled", :link "", :ms-per-beat 500.0, :lowest-beat 1/4, :audio "", :tempo 120}, :data [[{:duration 3/4, :notes {:atom {:keyword "Chord", :init {:arguments ["F#m"]}}}} nil nil {:duration 3/4, :notes {:atom {:keyword "Chord", :init {:arguments ["E"]}}}}] [nil nil {:duration 3/2, :notes {:atom {:keyword "Chord", :init {:arguments ["D"]}}}} nil] [nil nil nil nil]]}]
        (is (= want (compile-track tree)))))
  (testing "advanced"
    ; TODO: Consider moving to `normalization` suite
    (testing "beat duration exceeds single measure"
      (let [tree [:track [:statement [:header [:meta "Tempo"] [:number "100"]]]
                         [:statement [:header [:meta "Time"] [:meter [:number "3"] [:number "4"]]] [:play [:list [:pair [:div [:number "6"] [:number "4"]] [:set [:atom [:keyword "Scale"] [:init [:arguments [:string "'C# phrygian'"]]]] [:atom [:keyword "Chord"] [:init [:arguments [:string "'C#m'"]]]]]] [:pair [:div [:number "6"] [:number "4"]] [:atom [:keyword "Chord"] [:init [:arguments [:string "'Dmaj7'"]]]]]]]]]
            want {:headers {:tags [],
                            :desc "",
                            :time [3 4],
                            :total-beats 3N,
                            :title "Untitled",
                            :link "",
                            :ms-per-beat 1200.0,
                            :lowest-beat 1/2,
                            :audio "",
                            :tempo 100},
                  :data [[{:duration 3/2,
                           :notes [{:atom {:keyword "Scale",
                                           :init {:arguments ["C# phrygian"]}}}
                                   {:atom {:keyword "Chord",
                                           :init {:arguments ["C#m"]}}}]}
                          nil]
                         [nil
                          {:duration 3/2,
                           :notes {:atom {:keyword "Chord",
                                          :init {:arguments ["Dmaj7"]}}}}]
                         [nil nil]]}]
      (is (= want (compile-track tree)))))))
  ; TODO: Move this to `normalize-measures`, since this is what's breaking
  ;  - Actually, this stems from `lowest-beat` returning 1/4 when it should be 1/8
  (testing "obtuse meter (e.g. 5|8, 3|8)"
      (let [tree [:track [:statement [:header [:meta "Tempo"] [:number "75"]]] [:statement [:header [:meta "Time"] [:meter [:number "5"] [:number "8"]]] [:play [:list [:pair [:div [:number "3"] [:number "8"]] [:set [:atom [:keyword "Scale"] [:init [:arguments [:string "'D dorian'"]]]] [:atom [:keyword "Chord"] [:init [:arguments [:string "'Dm9'"]]]]]] [:pair [:div [:number "2"] [:number "8"]] [:atom [:keyword "Chord"] [:init [:arguments [:string "'Am9'"]]]]]]]]]
            want {:headers {:tags [], :desc "", :time [5 8], :total-beats 5/8, :title "Untitled", :link "", :ms-per-beat 400.0, :lowest-beat 1/8, :audio "", :tempo 75}, :data [[{:duration 3/8, :notes [{:atom {:keyword "Scale", :init {:arguments ["D dorian"]}}} {:atom {:keyword "Chord", :init {:arguments ["Dm9"]}}}]} nil nil {:duration 1/4, :notes {:atom {:keyword "Chord", :init {:arguments ["Am9"]}}}} nil]]}]
      (is (= want (compile-track tree))))))
