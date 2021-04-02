(ns bach.v3-compose-test
  (:require #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer-macros [deftest is testing run-tests]])
            [bach.compose :as compose]
            [bach.data :refer [to-ratio]]))

; @see https://cljdoc.org/d/leiningen/leiningen/2.9.5/api/leiningen.test
; (deftest ^:v3 normalization
(deftest normalization
  (testing "collection-tree"
    (let [tree [:list
                [:pair
                 [:number 1]
                 [:identifier :a]]
                [:set
                 [:identifier :b]
                 [:identifier :c]]
                [:set
                  [:identifier :d]
                  [:list
                   [:identifier :e]
                   [:identifier :f]]]]
          want [{:duration
                  [:number 1],
                 :elements
                  [:identifier :a]}
                 #{[:identifier :b] [:identifier :c]}
                 #{[:identifier :d] [[:identifier :e] [:identifier :f]]}]]
      (is (= want (compose/normalize-collection-tree tree))))))
      ; (is (= want (compose/reduce-durations tree))))))

; (deftest defaults
;   (testing "tempo"
;     (is (= 120 track/default-tempo)))
;   (testing "meter"
;     (is (= [4 4] track/default-meter))))

; ; TODO: change the values from strings to keywords, naturally supported in clojure. derp.
; (deftest validation
;   (testing "assignment"
;     (let [tree [:track [:statement [:assign [:identifier ":Test"] [:number "1"]]]]]
;       (is (= (track/validate tree) true))))
;   (testing "identifier (valid, known variable)"
;     (let [tree [:track
;                 [:statement [:assign [:identifier ":A"] [:number "1"]]]
;                 [:statement [:assign [:identifier ":B"] [:identifier ":A"]]]]]
;       (is (= (track/validate tree) true))))
;   (testing "identifier (invalid, unknown variable)"
;     (let [tree [:track
;                 [:statement
;                  [:assign [:identifier ":A"] [:number "1"]]
;                  [:assign [:identifier "B"] [:identifier "Z"]]]]]
;       (is (thrown-with-msg? #?(:clj Exception :cljs js/Error) #"Variable is not declared before it's used" (track/validate tree)))))
;   (testing "basic div (valid numerator and denominator)"
;     (let [tree [:track [:statement [:div [:number "1"] [:number "2"]]]]]
;       (is (= (track/validate tree) true))))
;   (testing "basic div (valid numerator, invalid denominator)"
;     (let [tree [:track [:statement [:div [:number "1"] [:number "3"]]]]]
;       (is (thrown-with-msg? #?(:clj Exception :cljs js/Error) #"Note divisors must be even and no greater than 512" (track/validate tree)))))
;   (testing "lists"
;     (testing "flat"
;       (let [tree [:track [:statement [:list [:number "1"] [:number "2"]]]]]
;         (is (= (track/validate tree) true))))))
;     ; TODO: Add this check to `bach.track/validate`. This restriction makes things simpler for everybody.
;     ; (testing "nested"
;     ;   (let [tree [:track
;     ;               [:statement
;     ;                [:list
;     ;                 [:number "1"]
;     ;                 [:number "2"]
;     ;                 [:list
;     ;                  [:number "3"]
;     ;                  [:number "4"]]]]]]
;     ;     (is (thrown-with-msg? Exception #"lists cannot be nested in other lists" (validate tree)))))))

; (deftest reduction
;   (testing "number"
;     (let [tree [:track [:statement [:div [:number "1"] [:number "4"]]]]
;           want [:track [:statement (/ 1 4)]]]
;       (is (= want (track/reduce-values tree)))))
;   (testing "string"
;     (let [tree [:track [:statement [:string "'Text'"]]]
;           want [:track [:statement "Text"]]]
;       (is (= want (track/reduce-values tree)))))
;   (testing "operation"
;     (let [tree [:track
;                 [:statement
;                  [:add
;                   [:div [:number "1"] [:number "2"]]
;                   [:div [:number "1"] [:number "4"]]]]]
;           want [:track [:statement (/ 3 4)]]]
;       (is (= want (track/reduce-values tree))))))

; ; FIXME: nested transitive variables are broken (:A = 1, :B = :A, :C = :B)
; (deftest dereference
;   (testing "variables (simple)"
;     (let [tree [:track
;                 [:statement [:assign [:identifier :A] [:number 1]]]
;                 [:statement [:assign [:identifier :B] [:identifier :A]]]]
;           want [:track
;                 [:statement [:assign [:identifier :A] [:number 1]]]
;                 [:statement [:assign [:identifier :B] [:number 1]]]]]
;       (is (= want (track/deref-variables tree)))))
;   (testing "variables (nested)"
;     (let [tree [:track [:statement [:assign [:identifier :A] [:number 1]]]
;                 [:statement [:assign [:identifier :B] [:identifier :A]]]
;                 [:statement [:assign [:identifier :C] [:identifier :B]]]]
;           want [:track
;                 [:statement [:assign [:identifier :A] [:number 1]]]
;                 [:statement [:assign [:identifier :B] [:number 1]]]
;                 [:statement [:assign [:identifier :C] [:number 1]]]]]
;       (is (= want (track/deref-variables tree)))))
;   (testing "variables (list/array)"
;     (let [tree [:track
;                 [:statement
;                  [:assign [:identifier :A] [:list [:number 1] [:number 2]]]]
;                 [:statement [:assign [:identifier :B] [:identifier :A]]]]
;           want [:track
;                 [:statement
;                  [:assign [:identifier :A] [:list [:number 1] [:number 2]]]]
;                 [:statement
;                  [:assign [:identifier :B] [:list [:number 1] [:number 2]]]]]]
;       (is (= want (track/deref-variables tree)))))
;   (testing "variables (nested list/array)"
;     (let [tree [:track
;                 [:statement
;                  [:assign
;                   [:identifier :A]
;                   [:atom
;                    [:keyword "Chord"]
;                    [:arguments [:string "'D2min7'"]]]]]
;                 [:statement
;                  [:assign
;                   [:identifier :B]
;                   [:list [:pair [:number "1"] [:identifier :A]]]]]]
;           want [:track
;                 [:statement
;                  [:assign
;                   [:identifier :A]
;                   [:atom
;                    [:keyword "Chord"]
;                    [:arguments [:string "'D2min7'"]]]]]
;                 [:statement
;                  [:assign
;                   [:identifier :B]
;                   [:list
;                    [:pair
;                     [:number "1"]
;                     [:atom
;                      [:keyword "Chord"]
;                      [:arguments [:string "'D2min7'"]]]]]]]]]
;       (is (= want (track/deref-variables tree)))))
;   ; (testing "beats"
;   ;   (let [tree [:track [:statement [:assign [:identifier ":ABC"] [:list [:pair [:number "1"] [:atom [:keyword "Chord"] [:init [:arguments [:string "'D2min7'"]]]]] [:pair [:number "3"] [:atom [:keyword "Chord"] [:init [:arguments [:string "'B2Maj7'"]]]]]]]]]
;   ;         want [:track [:statement [:assign [:identifier ":ABC"] [:list [:beat [:list [:atom [:keyword "Chord"] [:init [:arguments [:string "'D2min7'"]]]]]] [:beat []] [:beat [:atom [:keyword "Chord"] [:init [:arguments [:string "'B2Maj7'"]]]]]]]]]]
;   ;     (is (= true true))))) ; FIXME/TODO
;   )

; ; TODO
; ; (deftest finds-tempo
; ;   )

; ; TODO
; ; (deftest finds-meter)

; (deftest pulse-beat
;   (testing "whole number"
;     (let [tree [:track
;                 [:statement
;                  [:list
;                   [:pair [:number "4"] [:list]]
;                   [:pair [:number "2"] [:list]]
;                   [:pair [:number "1"] [:list]]]]]
;           want 1]
;       (is (= want (track/get-pulse-beat tree)))))
;   (testing "ratio"
;     (let [tree [:track
;                 [:statement
;                  [:list
;                   [:pair [:div [:number "1"] [:number "2"]] [:list]]
;                   [:pair [:div [:number "1"] [:number "4"]] [:list]]
;                   [:pair [:div [:number "1"] [:number "8"]] [:list]]]]]
;           want (/ 1 8)]
;       (is (= want (track/get-pulse-beat tree)))))
;   (testing "complex ratio"
;     (let [tree [:track
;                 [:statement
;                  [:header
;                   [:meta "Meter"]
;                   [:meter [:number "3"] [:number "4"]]]]
;                 [:statement
;                  [:list
;                   [:pair
;                    [:add
;                     [:div [:number "1"] [:number "2"]]
;                     [:div [:number "1"] [:number "4"]]]
;                    [:atom
;                     [:keyword "Note"]
;                     [:arguments [:string "'C2'"]]]]]]]
;           want (/ 3 4)]
;       (is (= want (track/get-pulse-beat tree)))))
;   (testing "misaligned to ratio"
;     (let [tree [:track
;                 [:statement
;                  [:list
;                   [:pair
;                    [:div [:number "3"] [:number "8"]]
;                    [:atom
;                     [:keyword "Note"]
;                     [:arguments [:string "'C2'"]]]]
;                   [:pair
;                    [:div [:number "5"] [:number "8"]]
;                    [:atom
;                     [:keyword "Note"]
;                     [:arguments [:string "'E2'"]]]]]]]
;           want (/ 1 8)]
;       (is (= want (track/get-pulse-beat tree)))))
;   (testing "spanning multiple measures"
;     (testing "aligned"
;       (let [tree [:track
;                   [:statement
;                    [:header
;                     [:meta "Meter"]
;                     [:meter [:number "6"] [:number "8"]]]]
;                   [:statement
;                    [:list
;                     [:pair
;                      [:mul
;                       [:number "2"]
;                       [:div [:number "6"] [:number "8"]]]
;                      [:atom
;                       [:keyword "Note"]
;                       [:arguments [:string "'C2'"]]]]]]]
;             ; TODO: Eventually, once get-pulse-beat can support multiple measures via ##Inf (Clojure 1.9.946+)
;             ; - Probably abandoning this since things are easier if pulse-beat cannot exceed a measure (e.g. 1)
;             ; want 3/2
;             want (/ 3 4)]
;         (is (= want (track/get-pulse-beat tree)))))
;     (testing "aligned (alt)"
;       (let [tree [:track
;                   [:statement
;                    [:header
;                     [:meta "Meter"]
;                     [:meter [:number "3"] [:number "4"]]]]
;                   [:statement
;                    [:list
;                     [:pair
;                      [:div [:number "6"] [:number "4"]]
;                      [:atom
;                       [:keyword "Note"]
;                       [:arguments [:string "'C2'"]]]]]]]
;             ; LAST
;             ; - Only want if we support lowest-common-beat exceeding an entire measure (seems too complicated right now)
;             ; want 6/4]
;             want (/ 3 4)]
;         (is (= want (track/get-pulse-beat tree)))))
;     (testing "misaligned"
;       (let [tree [:track
;                   [:statement
;                    [:header
;                     [:meta "Meter"]
;                     [:meter [:number "6"] [:number "8"]]]]
;                   [:statement
;                    [:list
;                     [:pair
;                      [:number "2"]
;                      [:atom
;                       [:keyword "Note"]
;                       [:arguments [:string "'C2'"]]]]]]]
;             want (/ 1 8)]
;         (is (= want (track/get-pulse-beat tree)))))))

; (deftest total-beats
;   (testing "common meter"
;     (let [tree [:track
;                 [:statement
;                  [:list
;                   [:pair [:number "1"] [:list]]
;                   [:pair [:number "4"] [:list]]
;                   [:pair [:div [:number "1"] [:number "4"]] [:list]]
;                   [:pair [:div [:number "1"] [:number "4"]] [:list]]]]]
;           want (to-ratio 5.5)]
;       (is (= want (track/get-total-beats tree)))))
;   (testing "uncommon meter"
;     (let [tree [:track
;                 [:statement
;                  [:meta "Meter"]
;                  [:meter [:number "6"] [:number "8"]]]
;                 [:statement
;                  [:list
;                   [:pair [:number "1"] [:list]]
;                   [:pair [:div [:number "1"] [:number "8"]] [:list]]]]]
;           want (/ 9 8)]
;       (is (= want (track/get-total-beats tree))))))

; (deftest normalized-total-beats
;   (testing "beat unit is less (shorter) than lowest common beat"
;     (testing "single measure"
;       (let [tree [:track
;                   [:statement
;                    [:assign
;                     [:identifier ":ABC"]
;                     [:list
;                      [:pair
;                       [:number "1"]
;                       [:atom
;                        [:keyword "Chord"]
;                        [:arguments [:string "'D2min7'"]]]]]]]]
;             want 1]
;         (is (= want (track/get-normalized-total-beats tree)))))
;     (testing "multiple measures"
;       (let [tree [:track
;                   [:statement
;                    [:assign
;                     [:identifier ":ABC"]
;                     [:list
;                      [:pair
;                       [:number "1"]
;                       [:atom
;                        [:keyword "Chord"]
;                        [:arguments [:string "'D2min7'"]]]]
;                      [:pair
;                       [:number "1"]
;                       [:atom
;                        [:keyword "Chord"]
;                        [:arguments [:string "'G3maj7'"]]]]]]]]
;             want 2]
;         (is (= want (track/get-normalized-total-beats tree))))))
;   (testing "beat unit is greater (longer) than lowest common beat"
;     (testing "single measure"
;       (let [tree [:track
;                   [:statement
;                    [:assign
;                     [:identifier ":ABC"]
;                     [:list
;                      [:pair
;                       [:div
;                        [:number "1"]
;                        [:number "8"]]
;                       [:atom
;                        [:keyword "Chord"]
;                        [:arguments [:string "'D2min7'"]]]]]]]]
;             want 1]
;         (is (= want (track/get-normalized-total-beats tree)))))
;     (testing "multiple measures"
;       (let [tree [:track
;                   [:statement
;                    [:assign
;                     [:identifier ":ABC"]
;                     [:list
;                      [:pair
;                       [:number "1"]
;                       [:atom
;                        [:keyword "Chord"]
;                        [:arguments [:string "'D2min7'"]]]]
;                      [:pair
;                       [:div
;                        [:number "1"]
;                        [:number "8"]]
;                       [:atom
;                        [:keyword "Chord"]
;                        [:arguments [:string "'G3maj7'"]]]]]]]]
;             want 9]
;         (is (= want (track/get-normalized-total-beats tree)))))))

; (deftest normalized-total-measures
;   (testing "common meter"
;     (testing "beat unit matches lowest common beat"
;       (let [tree [:track
;                   [:statement
;                    [:list
;                     [:pair [:number "1"] [:list]]
;                     [:pair [:number "4"] [:list]]
;                     [:pair [:div [:number "1"] [:number "4"]] [:list]]
;                     [:pair [:div [:number "1"] [:number "4"]] [:list]]]]]
;             want (to-ratio 5.5)]
;         (is (= want (track/get-normalized-total-measures tree)))))
;     (testing "beat unit is less (shorter) than lowest common beat"
;       (let [tree [:track
;                   [:statement
;                    [:list
;                     [:pair [:div [:number "1"] [:number "2"]] [:list]]
;                     [:pair [:div [:number "1"] [:number "2"]] [:list]]]]]
;             want 1]
;         (is (= want (track/get-normalized-total-measures tree)))))
;     (testing "beat unit is greater (longer) than lowest common beat"
;       (let [tree [:track
;                   [:statement
;                    [:list
;                     [:pair [:div [:number "1"] [:number "8"]] [:list]]
;                     [:pair [:div [:number "1"] [:number "8"]] [:list]]
;                     [:pair [:div [:number "1"] [:number "8"]] [:list]]
;                     [:pair [:div [:number "1"] [:number "8"]] [:list]]]]]
;             want (to-ratio 0.5)]
;         (is (= want (track/get-normalized-total-measures tree)))))
;     (testing "total measures is less than duration of full bar"
;       (let [tree [:track
;                   [:statement
;                    [:list
;                     [:pair [:div [:number "1"] [:number "8"]] [:list]]
;                     [:pair [:div [:number "1"] [:number "8"]] [:list]]]]]
;             want (to-ratio 0.25)]
;         (is (= want (track/get-normalized-total-measures tree))))))
;   (testing "less common meters"
;     (testing "beat unit matches lowest common beat"
;       (let [tree [:track
;                   [:statement
;                    [:header
;                     [:meta "Meter"]
;                     [:meter [:number "3"] [:number "4"]]]]
;                   [:statement
;                    [:list
;                     [:pair [:div [:number "3"] [:number "4"]] [:list]]
;                     [:pair [:div [:number "1"] [:number "4"]] [:list]]
;                     [:pair [:div [:number "1"] [:number "4"]] [:list]]
;                     [:pair [:div [:number "1"] [:number "4"]] [:list]]]]]
;             want 2]
;         (is (= want (track/get-normalized-total-measures tree)))))
;     (testing "beat unit is less (shorter) than lowest common beat"
;       (let [tree [:track
;                   [:statement
;                    [:header
;                     [:meta "Meter"]
;                     [:meter [:number "3"] [:number "4"]]]]
;                   [:statement
;                    [:list
;                     [:pair [:div [:number "1"] [:number "8"]] [:list]]
;                     [:pair [:div [:number "1"] [:number "8"]] [:list]]
;                     [:pair [:div [:number "1"] [:number "4"]] [:list]]
;                     [:pair [:div [:number "1"] [:number "4"]] [:list]]]]]
;             want 1]
;         (is (= want (track/get-normalized-total-measures tree)))))
;     (testing "beat unit is greater (longer) than lowest common beat"
;       (let [tree [:track
;                   [:statement
;                    [:header
;                     [:meta "Meter"]
;                     [:meter [:number "3"] [:number "4"]]]]
;                   [:statement
;                    [:list
;                     [:pair [:div [:number "1"] [:number "8"]] [:list]]
;                     [:pair [:div [:number "1"] [:number "8"]] [:list]]
;                     [:pair [:div [:number "1"] [:number "8"]] [:list]]
;                     [:pair [:div [:number "1"] [:number "8"]] [:list]]
;                     [:pair [:div [:number "1"] [:number "8"]] [:list]]
;                     [:pair [:div [:number "1"] [:number "8"]] [:list]]]]]
;             want 1]
;         (is (= want (track/get-normalized-total-measures tree)))))
;     (testing "total measures is less than duration of full bar"
;       (let [tree [:track
;                   [:statement
;                    [:header
;                     [:meta "Meter"]
;                     [:meter [:number "3"] [:number "4"]]]]
;                   [:statement
;                    [:list
;                     [:pair [:div [:number "1"] [:number "8"]] [:list]]
;                     [:pair [:div [:number "1"] [:number "8"]] [:list]]]]]
;             want (/ 1 3)]
;         (is (= want (track/get-normalized-total-measures tree)))))))

; ; TODO
; ; (deftest get-normalized-beats-per-measure)

; (deftest normalized-duration
;   (testing "common meter"
;     (testing "beat unit matches lowest common beat"
;       (let [duration (/ 1 2)
;             pulse-beat (/ 1 4)
;             meter (/ 4 4)
;             want 2]
;         (is (= want (track/normalize-duration duration pulse-beat meter)))))
;     (testing "beat unit is less (shorter) than lowest common beat"
;       (let [duration 1
;             pulse-beat 1
;             meter (/ 2 4)
;             want 2]
;         (is (= want (track/normalize-duration duration pulse-beat meter)))))
;     (testing "beat unit is greater (longer) than lowest common beat"
;       (testing "common case"
;         (let [duration (/ 1 2)
;               pulse-beat (/ 1 8)
;               meter 1
;               want 4]
;           (is (= want (track/normalize-duration duration pulse-beat meter)))))
;       (testing "less common case"
;         (let [duration (/ 1 2)
;               pulse-beat (/ 1 8)
;               meter (/ 2 4)
;               want 4]
;           (is (= want (track/normalize-duration duration pulse-beat meter)))))
;       (testing "and duration misaligns with meter"
;         (let [duration (/ 3 8)
;               pulse-beat (/ 1 8)
;               meter 1
;               want 3]
;           (is (= want (track/normalize-duration duration pulse-beat meter)))))
;       (testing "1/16"
;         (let [duration 9
;               pulse-beat (/ 1 16)
;               meter 1
;               want 144]
;           (is (= want (track/normalize-duration duration pulse-beat meter)))))))
;   (testing "less common meters"
;     (testing "duration matches full bar"
;       (let [duration (/ 5 8)
;             pulse-beat (/ 1 8)
;             meter (/ 5 8)
;             want 5]
;         (is (= want (track/normalize-duration duration pulse-beat meter)))))
;     (testing "duration is less than full bar (even meter)"
;       (let [duration (/ 4 8)
;             pulse-beat (/ 1 8)
;             meter (/ 6 8)
;             want 4]
;         (is (= want (track/normalize-duration duration pulse-beat meter)))))
;     (testing "duration is less than full bar (odd meter)"
;       (let [duration (/ 3 8)
;             pulse-beat (/ 1 8)
;             meter (/ 5 8)
;             want 3]
;         (is (= want (track/normalize-duration duration pulse-beat meter)))))
;     (testing "duration is greater than full bar and pulse-beat equals a full bar"
;       (let [duration (/ 6 4)
;             pulse-beat (/ 3 4)
;             meter (/ 3 4)
;             want 2]
;         (is (= want (track/normalize-duration duration pulse-beat meter)))))
;     (testing "duration is less than pulse-beat (edge case)"
;       (let [duration (/ 1 16)
;             pulse-beat (/ 1 8)
;             meter (/ 6 8)
;             want (/ 1 2)]
;         (is (= want (track/normalize-duration duration pulse-beat meter)))))
;     (testing "beats per measure is greater than beat unit"
;       (let [duration (/ 9 8)
;             pulse-beat (/ 1 8)
;             meter (/ 12 8)
;             want 9]
;         (is (= want (track/normalize-duration duration pulse-beat meter)))))))

; (deftest duration
;   (testing "minutes"
;     (testing "no extra seconds"
;       (let [tree [:track
;                   [:statement
;                    [:list
;                     [:pair [:number "30"] [:list]]]]] ; 30 measures x 4 beats = 120 beats
;             want 1]
;         (is (= want (track/get-total-duration tree :minutes)))))
;     (testing "with extra seconds"
;       ; 20 measures x 4 beats = 80, 25 measures x 4 = 100 beats === 180 total beats
;       (let [tree [:track
;                   [:statement
;                    [:list
;                     [:pair [:number "20"] [:list]]
;                     [:pair [:number "25"] [:list]]]]]
;             want (to-ratio 1.5)]
;         (is (= want (track/get-total-duration tree :minutes))))))
;   (testing "milliseconds"
;     (let [tree [:track
;                 [:statement
;                  [:list
;                   [:pair [:number "30"] [:list]]]]] ; 30 measures x 4 beats = 120 beats
;           want 60000]
;       (is (= want (track/get-total-duration tree :milliseconds))))))

; ; FIXME: if the duration of the beats is less than a measure, it ends up breaking
; ; the get-normalized-ms-per-beat calculation (stemming from total-duration-ms). Need a test for this!
; (deftest milliseconds-per-beat
;   (testing "singleton"
;     (testing "whole note"
;       (let [tree [:track
;                   [:statement
;                    [:list
;                     [:pair
;                      [:number "1"]
;                      [:atom
;                       [:keyword "Note"]
;                       [:arguments [:string "'C2'"]]]]]]]
;             want 2000.0]
;         (is (= want (track/get-normalized-ms-per-beat tree)))))
;     (testing "half note"
;       (let [tree [:track
;                   [:statement
;                    [:list
;                     [:pair
;                      [:div
;                       [:number "1"]
;                       [:number "2"]]
;                      [:atom
;                       [:keyword "Note"]
;                       [:arguments [:string "'C2'"]]]]
;                     [:pair
;                      [:div
;                       [:number "1"]
;                       [:number "2"]]
;                      [:atom
;                       [:keyword "Note"]
;                       [:arguments [:string "'C2'"]]]]]]]
;             want 1000.0]
;         (is (= want (track/get-normalized-ms-per-beat tree)))))
;     (testing "quarter note"
;       (let [tree [:track
;                   [:statement
;                    [:list
;                     [:pair
;                      [:div
;                       [:number "1"]
;                       [:number "4"]]
;                      [:atom
;                       [:keyword "Note"]
;                       [:arguments [:string "'C2'"]]]]]]]
;             want 500.0]
;         (is (= want (track/get-normalized-ms-per-beat tree)))))
;     (testing "eigth note"
;       (let [tree [:track
;                   [:statement
;                    [:list
;                     [:pair
;                      [:div
;                       [:number "1"]
;                       [:number "8"]]
;                      [:atom
;                       [:keyword "Note"]
;                       [:arguments [:string "'C2'"]]]]]]]
;             want 250.0]
;         (is (= want (track/get-normalized-ms-per-beat tree))))))
;   ; FIXME: Could improve this by having `get-pulse-beat` look at the reduced values, right now it is using 1/4 when it could use 3/4 if it added 1/2 and 1/4 together first (this should already be happening, actually)
;   (testing "meter"
;     (testing "simple"
;       (testing "3/4"
;         (let [tree [:track
;                     [:statement
;                      [:header
;                       [:meta "Meter"]
;                       [:meter [:number "3"] [:number "4"]]]]
;                     [:statement
;                      [:list
;                       [:pair
;                        [:add
;                         [:div [:number "1"] [:number "2"]]
;                         [:div [:number "1"] [:number "4"]]]
;                        [:atom
;                         [:keyword "Note"]
;                         [:init [:string "'C2'"]]]]]]]
;               want 1500.0]
;           (is (= want (track/get-normalized-ms-per-beat tree))))))
;     (testing "mixed"
;       (testing "6/8"
;         (let [tree [:track
;                     [:statement
;                      [:header
;                       [:meta "Meter"]
;                       [:meter [:number "6"] [:number "8"]]]]
;                     [:statement
;                      [:list
;                       [:pair
;                        [:number "1"]
;                        [:atom
;                         [:keyword "Note"]
;                         [:arguments [:string "'C2'"]]]]]]]
;               ; Because "1", or 4 beats, does not align flushly with the meter (uses 1/8 as pulse beat in this case)
;               want 500.0]
;           (is (= want (track/get-normalized-ms-per-beat tree)))))
;       (testing "3/8"
;         (let [tree [:track
;                     [:statement
;                      [:header
;                       [:meta "Meter"]
;                       [:meter [:number "3"] [:number "8"]]]]
;                     [:statement
;                      [:list
;                       [:pair
;                        [:div [:number "1"] [:number "8"]]
;                        [:atom
;                         [:keyword "Note"]
;                         [:arguments [:string "'C2'"]]]]]]]
;               want 500.0]
;           (is (= want (track/get-normalized-ms-per-beat tree)))))
;       (testing "5/8"
;         (let [tree [:track
;                     [:statement
;                      [:header
;                       [:meta "Meter"]
;                       [:meter [:number "5"] [:number "8"]]]]
;                     [:statement
;                      [:list
;                       [:pair
;                        [:div [:number "1"] [:number "8"]]
;                        [:atom
;                         [:keyword "Note"]
;                         [:arguments [:string "'C2'"]]]]]]]
;               want 500.0]
;           (is (= want (track/get-normalized-ms-per-beat tree)))))
;       (testing "compound"
;         (testing "9/8"
;           (let [tree [:track
;                       [:statement
;                        [:header
;                         [:meta "Meter"]
;                         [:meter [:number "9"] [:number "8"]]]]
;                       [:statement
;                        [:list
;                         [:pair
;                          [:div [:number "1"] [:number "8"]]
;                          [:atom
;                           [:keyword "Note"]
;                           [:arguments [:string "'C2'"]]]]]]]
;                 want 500.0]
;             (is (= want (track/get-normalized-ms-per-beat tree)))))
;         (testing "12/8"
;           (let [tree [:track
;                       [:statement
;                        [:header
;                         [:meta "Meter"]
;                         [:meter [:number "12"] [:number "8"]]]]
;                       [:statement
;                        [:list
;                         [:pair
;                          [:div [:number "1"] [:number "8"]]
;                          [:atom
;                           [:keyword "Note"]
;                           [:arguments [:string "'C2'"]]]]]]]
;                 want 500.0]
;             (is (= want (track/get-normalized-ms-per-beat tree))))))
;       (testing "complex"
;         (testing "5/4"
;           (let [tree [:track
;                       [:statement
;                        [:header
;                         [:meta "Meter"]
;                         [:meter [:number "5"] [:number "4"]]]]
;                       [:statement
;                        [:list
;                         [:pair
;                          [:div [:number "1"] [:number "4"]]
;                          [:atom
;                           [:keyword "Note"]
;                           [:init [:arguments [:string "'C2'"]]]]]]]]
;                 want 500.0]
;             (is (= want (track/get-normalized-ms-per-beat tree)))))
;         (testing "7/8"
;           (let [tree [:track
;                       [:statement
;                        [:header
;                         [:meta "Meter"]
;                         [:meter [:number "7"] [:number "8"]]]]
;                       [:statement
;                        [:list
;                         [:pair
;                          [:div [:number "1"] [:number "8"]]
;                          [:atom
;                           [:keyword "Note"]
;                           [:arguments [:string "'C2'"]]]]]]]
;                 want 500.0]
;             (is (= want (track/get-normalized-ms-per-beat tree)))))))))

; ; TODO: Improve organization of this test, inconsistent (should be method-first)
; (deftest scaled
;   (testing "beats per measure"
;     (testing "fully reduced ratio"
;       (let [tree [:track
;                   [:statement
;                    [:header
;                     [:meta "Meter"]
;                     [:meter [:number "3"] [:number "4"]]]]]
;             want 3]
;         (is (= want (track/get-beats-per-measure tree)))))
;     (testing "reduceable ratio (avoid reduction)"
;       (let [tree [:track
;                   [:statement
;                    [:header
;                     [:meta "Meter"]
;                     [:meter [:number "6"] [:number "8"]]]]]
;             want 6]
;         (is (= want (track/get-beats-per-measure tree)))))))

; (deftest normalized-measures
;   (testing "with whole notes"
;     (let [tree [:track
;                 [:statement
;                  [:assign
;                   [:identifier ":ABC"]
;                   [:list
;                    [:pair
;                     [:number "1"]
;                     [:atom
;                      [:keyword "Chord"]
;                      [:arguments [:string "'D2min7'"]]]]
;                    [:pair
;                     [:number "1"]
;                     [:atom
;                      [:keyword "Chord"]
;                      [:arguments [:string "'G2Maj7'"]]]]
;                    [:pair
;                     [:number "1"]
;                     [:atom
;                      [:keyword "Chord"]
;                      [:arguments [:string "'C2maj7'"]]]]]]]
;                 [:statement
;                  [:play [:identifier ":ABC"]]]]
;           want [[{:duration 1,
;                   :items [{:keyword "Chord",
;                            :arguments ["D2min7"]}]}]
;                 [{:duration 1,
;                   :items [{:keyword "Chord",
;                            :arguments ["G2Maj7"]}]}]
;                 [{:duration 1,
;                   :items [{:keyword "Chord",
;                            :arguments ["C2maj7"]}]}]]]
;       (is (= want (track/normalize-measures tree)))))
;   (testing "with half notes"
;     (let [tree [:track
;                 [:statement
;                  [:assign
;                   [:identifier ":ABC"]
;                   [:list
;                    [:pair
;                     [:div [:number "1"] [:number "2"]]
;                     [:atom
;                      [:keyword "Chord"]
;                      [:arguments [:string "'D2min7'"]]]]
;                    [:pair
;                     [:div [:number "1"] [:number "2"]]
;                     [:atom
;                      [:keyword "Chord"]
;                      [:arguments [:string "'G2Maj7'"]]]]
;                    [:pair
;                     [:number "1"]
;                     [:atom
;                      [:keyword "Chord"]
;                      [:arguments [:string "'C2maj7'"]]]]]]]
;                 [:statement [:play [:identifier ":ABC"]]]]
;           want [[{:duration 1,
;                   :items [{:keyword "Chord",
;                            :arguments ["D2min7"]}]}
;                  {:duration 1,
;                   :items [{:keyword "Chord",
;                            :arguments ["G2Maj7"]}]}]
;                 [{:duration 2,
;                   :items [{:keyword "Chord",
;                            :arguments ["C2maj7"]}]}
;                  nil]]]
;       (is (= want (track/normalize-measures tree)))))
;   (testing "with quarter notes"
;     (let [tree [:track
;                 [:statement
;                  [:assign
;                   [:identifier ":ABC"]
;                   [:list
;                    [:pair
;                     [:div [:number "1"] [:number "4"]]
;                     [:atom
;                      [:keyword "Chord"]
;                      [:arguments [:string "'D2min7'"]]]]
;                    [:pair
;                     [:div [:number "1"] [:number "2"]]
;                     [:atom
;                      [:keyword "Chord"]
;                      [:arguments [:string "'G2Maj7'"]]]]
;                    [:pair
;                     [:div [:number "1"] [:number "4"]]
;                     [:atom
;                      [:keyword "Chord"]
;                      [:arguments [:string "'C2maj7'"]]]]]]]
;                 [:statement
;                  [:play [:identifier ":ABC"]]]]
;           want [[{:duration 1,
;                   :items [{:keyword "Chord",
;                            :arguments ["D2min7"]}]}
;                  {:duration 2,
;                   :items [{:keyword "Chord",
;                            :arguments ["G2Maj7"]}]}
;                  nil
;                  {:duration 1,
;                   :items [{:keyword "Chord",
;                            :arguments ["C2maj7"]}]}]]]
;       (is (= want (track/normalize-measures tree)))))
;   (testing "with eigth notes"
;     (let [tree [:track
;                 [:statement
;                  [:play
;                   [:list
;                    [:pair
;                     [:div
;                      [:number "5"]
;                      [:number "8"]]
;                     [:atom
;                      [:keyword "Chord"]
;                      [:arguments [:string "Dmin7"]]]]
;                    [:pair
;                     [:div
;                      [:number "3"]
;                      [:number "8"]]
;                     [:atom
;                      [:keyword "Chord"]
;                      [:arguments [:string "Emin7"]]]]]]]]
;           want [[{:duration 5,
;                   :items [{:keyword "Chord",
;                            :arguments ["Dmin7"]}]}
;                  nil
;                  nil
;                  nil
;                  nil
;                  {:duration 3,
;                   :items [{:keyword "Chord",
;                            :arguments ["Emin7"]}]}
;                  nil
;                  nil]]]
;       (is (= want (track/normalize-measures tree)))))
;   (testing "with unused trailing beats"
;     (testing "and total beats spans multiple measures"
;       (let [tree [:track
;                   [:statement
;                    [:play
;                     [:list
;                      [:pair
;                       [:div
;                        [:number "1"]
;                        [:number "2"]]
;                       [:atom
;                        [:keyword "Chord"]
;                        [:arguments [:string "'Cmin'"]]]]
;                      [:pair
;                       [:div
;                        [:number "1"]
;                        [:number "2"]]
;                       [:atom
;                        [:keyword "Chord"]
;                        [:arguments [:string "'G/B'"]]]]
;                      [:pair
;                       [:div
;                        [:number "1"]
;                        [:number "2"]]
;                       [:atom
;                        [:keyword "Chord"]
;                        [:arguments [:string "'Bb'"]]]]]]]]
;             want [[{:duration 1,
;                     :items [{:keyword "Chord",
;                              :arguments ["Cmin"]}]}
;                    {:duration 1,
;                     :items [{:keyword "Chord",
;                              :arguments ["G/B"]}]}]
;                   [{:duration 1,
;                     :items [{:keyword "Chord",
;                              :arguments ["Bb"]}]}]]]
;         (is (= want (track/normalize-measures tree)))))
;     (testing "and total beats is under a total measure"
;       (let [tree [:track
;                   [:statement
;                    [:play
;                     [:list
;                      [:pair
;                       [:div [:number "1"] [:number "2"]]
;                       [:atom
;                        [:keyword "Chord"]
;                        [:arguments [:string "'Cmin'"]]]]]]]]
;             want [[{:duration 1,
;                     :items [{:keyword "Chord",
;                              :arguments ["Cmin"]}]}
;                     ; TODO: Consider (from design perspective) if this trailing nil should be removed.
;                    nil]]]
;         (is (= want (track/normalize-measures tree)))))
;     (testing "when offset by non-trailing beat"
;       (let [tree [:track
;                   [:statement
;                    [:play
;                     [:list
;                      [:pair
;                       [:div [:number "1"] [:number "2"]]
;                       [:atom
;                        [:keyword "Chord"]
;                        [:arguments [:string "'Bb'"]]]]
;                      [:pair
;                       [:number "1"]
;                       [:atom
;                        [:keyword "Chord"]
;                        [:arguments [:string "'A7sus4'"]]]]
;                      [:pair
;                       [:number "1"]
;                       [:atom
;                        [:keyword "Chord"]
;                        [:arguments [:string "'A7'"]]]]]]]]
;             want [[{:duration 1,
;                     :items [{:keyword "Chord",
;                              :arguments ["Bb"]}]}
;                    {:duration 2,
;                     :items [{:keyword "Chord",
;                              :arguments ["A7sus4"]}]}]
;                   [nil
;                    {:duration 2,
;                     :items [{:keyword "Chord",
;                              :arguments ["A7"]}]}]
;                   [nil]]]
;         (is (= want (track/normalize-measures tree)))))))

; (deftest provisioning
;   (testing "headers"
;     (testing "static"
;       (testing "@Tempo"
;         (let [tree [:track
;                     [:statement
;                      [:header [:meta "Tempo"] [:number "90"]]]]
;               want 90]
;           (is (= want (:tempo (track/provision-headers tree))))))
;       (testing "@Meter"
;         (let [tree [:track
;                     [:statement
;                      [:header
;                       [:meta "Meter"]
;                       [:meter [:number "3"] [:number "4"]]]]]
;               want [3 4]]
;           (is (= want (:meter (track/provision-headers tree)))))))
;     (testing "dynamic"
;       (testing "total beats"
;         (let [tree [:track
;                     [:statement
;                      [:assign
;                       [:identifier ":ABC"]
;                       [:list
;                        [:pair [:number "1"] [:list]]
;                        [:pair [:number "3"] [:list]]]]]]
;               want 4]
;           (is (= want (:total-beats (track/provision-headers tree)))))))
;     (testing "ms per pulse beat"
;       (let [tree [:track
;                   [:statement
;                    [:assign
;                     [:identifier ":ABC"]
;                     [:list
;                      [:pair [:number "1"] [:list]]
;                      [:pair [:number "3"] [:list]]]]]]
;             want 2000.0]
;         (is (= want (:ms-per-pulse-beat (track/provision-headers tree))))))
;     (testing "pulse beat"
;       (let [tree [:track
;                   [:statement
;                    [:assign
;                     [:identifier ":ABC"]
;                     [:list
;                      [:pair
;                       [:div
;                        [:number "1"]
;                        [:number "4"]]
;                       [:list]]
;                      [:pair [:number "1"] [:list]]]]]]
;             want (/ 1 4)]
;         (is (= want (:pulse-beat (track/provision-headers tree))))))))
