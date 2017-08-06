(ns warble.track-test
  (:require [clojure.test :refer :all]
            [warble.lexer :refer :all]
            [warble.track :refer :all]))

(deftest defaults
  (testing "tempo"
    (is (= 120 default-tempo)))
  (testing "time-signature"
    (is (= [4 4] default-time-signature)))
  (testing "scale"
    (is (= "C2 Major" default-scale))))

; TODO: change the values from strings to keywords, naturally supported in clojure. derp.
(deftest validatation
  ; (testing "assignment"
  ;   (let [tree [:track [:statement [:assign [:identifier ":Test"] [:number "1"]]]]]
  ;     (is (= (validate tree) true))))
  (testing "identifier (valid, known variable)"
    (let [tree [:track [:statement [:assign [:identifier ":A"] [:number "1"]]]
                       [:statement [:assign [:identifier ":B"] [:identifier ":A"]]]]]
      (is (= (validate tree) true))))
  (testing "identifier (invalid, unknown variable)"
    (let [tree [:track [:statement [:assign [:identifier ":A"] [:number "1"]] [:assign [:identifier "B"] [:identifier "Z"]]]]]
      (is (thrown-with-msg? Exception #"variable is not declared before it's used" (validate tree)))))
  (testing "basic div (valid numerator and denominator)"
    (let [tree [:track [:statement [:div [:number "1"] [:number "2"]]]]]
      (is (= (validate tree) true))))
  (testing "basic div (valid numerator, invalid denominator)"
    (let [tree [:track [:statement [:div [:number "1"] [:number "3"]]]]]
      (is (thrown-with-msg? Exception #"note divisors must be base 2 and no greater than 512" (validate tree)))))
  (testing "basic div (invalid numerator, valid denominator)"
    (let [tree [:track [:statement [:div [:number "5"] [:number "4"]]]]]
      (is (thrown-with-msg? Exception #"numerator cannot be greater than denominator" (validate tree))))))
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
      (is (= want (reduce-values tree))))))

; FIXME: handle notes that aren't % 2
(deftest lowest-beat
  (testing "whole number"
    (let [tree [:track [:statement [:list [:pair [:number "4"] [:list]] [:pair [:number "2"] [:list]] [:pair [:number "1"] [:list]]]]]
          want 1]
      (is (= want (get-lowest-beat tree)))))
  (testing "ratio"
    (let [tree [:track [:statement [:list [:pair [:div [:number "1"] [:number "2"]] [:list]] [:pair [:div [:number "1"] [:number "4"]] [:list]] [:pair [:div [:number "1"] [:number "8"]] [:list]]]]]
          want (/ 1 8)]
      (is (= want (get-lowest-beat tree))))))

(deftest total-beats
  (testing "general"
    (let [tree [:track [:statement [:list [:pair [:number "1"] [:list]] [:pair [:number "4"] [:list]] [:pair [:div [:number "1"] [:number "4"]] [:list]] [:pair [:div [:number "1"] [:number "4"]] [:list]]]]]
          want (rationalize 5.5)]
      (is (= want (get-total-beats tree))))))

(deftest duration
  (testing "minutes"
    (testing "no extra seconds"
      (let [tree [:track [:statement [:list [:pair [:number "30"] [:list]]]]] ; 30 measures x 4 beats = 120 beats
            want 1]
        (is (= want (get-total-duration tree :minutes)))))
    (testing "with extra seconds"
      (let [tree [:track [:statement [:list [:pair [:number "20"] [:list]] [:pair [:number "25"] [:list]]]]] ; 20 measures x 4 beats = 80, 25 measures x 4 = 100 beats === 180 total beats
            want (rationalize 1.5)]
        (is (= want (get-total-duration tree :minutes))))))
  (testing "milliseconds"
    (let [tree [:track [:statement [:list [:pair [:number "30"] [:list]]]]] ; 30 measures x 4 beats = 120 beats
          want 60000]
      (is (= want (get-total-duration tree :milliseconds))))))

; FIXME: if the duration of the beats is less than a measure, it ends up breaking
; the get-ms-per-beat calculation (stemming from total-duration-ms). Need a test for this!
(deftest milliseconds-per-beat
  (testing "singleton"
    (testing "whole note"
      (let [tree [:track [:statement [:list [:pair [:number "1"] [:atom [:keyword "Note"] [:init [:arguments [:string "'C2'"]]]]]]]]
            want 2000.0] ; FIXME: I think that a quarter note should actually be 500. whole note should be 2000
        (is (= want (get-ms-per-beat tree)))))

    (testing "half note"
      (let [tree [:track [:statement [:list [:pair [:div [:number "1"] [:number "2"]] [:atom [:keyword "Note"] [:init [:arguments [:string "'C2'"]]]]] [:pair [:div [:number "1"] [:number "2"]] [:atom [:keyword "Note"] [:init [:arguments [:string "'C2'"]]]]]]]]
            want 1000.0]
        (is (= want (get-ms-per-beat tree)))))

    (testing "quarter note"
      (let [tree [:track [:statement [:list [:pair [:div [:number "1"] [:number "4"]] [:atom [:keyword "Note"] [:init [:arguments [:string "'C2'"]]]]]]]]
            want 500.0]
        (is (= want (get-ms-per-beat tree)))))

    (testing "eigth note"
      (let [tree [:track [:statement [:list [:pair [:div [:number "1"] [:number "8"]] [:atom [:keyword "Note"] [:init [:arguments [:string "'C2'"]]]]]]]]
            want 250.0]
        (is (= want (get-ms-per-beat tree)))))
))

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
  (testing "variables (simple)"
    (let [tree [:track [:statement [:assign [:identifier :A] [:number 1]]]
                       [:statement [:assign [:identifier :B] [:identifier :A]]]
                       [:statement [:assign [:identifier :C] [:identifier :B]]]]
          want [:track [:statement [:assign [:identifier :A] [:number 1]]]
                       [:statement [:assign [:identifier :B] [:number 1]]]
                       [:statement [:assign [:identifier :C] [:number 1]]]]]
      (is (= want (deref-variables tree)))))
  (testing "beats"
    (let [tree [:track [:statement [:assign [:identifier ":ABC"] [:list [:pair [:number "1"] [:atom [:keyword "Chord"] [:init [:arguments [:string "'D2min7'"]]]]] [:pair [:number "3"] [:atom [:keyword "Chord"] [:init [:arguments [:string "'B2Maj7'"]]]]]]]]]
          want [:track [:statement [:assign [:identifier ":ABC"] [:list [:beat [:list [:atom [:keyword "Chord"] [:init [:arguments [:string "'D2min7'"]]]]]] [:beat []] [:beat [:atom [:keyword "Chord"] [:init [:arguments [:string "'B2Maj7'"]]]]]]]]]]
      (is (= true true))))) ; FIXME/TODO

; FIXME: might need to consider the surrounding :statement, :assign, :track, etc.
; - eh, maybe not
; TODO: also test with derefrenced variables, need to make sure it doesn't go the same measure value N times (where N is the number of references)
(deftest normalization
  (testing "total beats"
    (testing "using greater unit than denominator in time signature"
      (testing "single measure"
        (let [tree [:track [:statement [:assign [:identifier ":ABC"] [:list [:pair [:number "1"] [:atom [:keyword "Chord"] [:init [:arguments [:string "'D2min7'"]]]]]]]]]
            want 4]
        (is (= want (get-normalized-total-beats tree)))))
      (testing "multiple measures"
        (let [tree [:track [:statement [:assign [:identifier ":ABC"] [:list [:pair [:number "1"] [:atom [:keyword "Chord"] [:init [:arguments [:string "'D2min7'"]]]]] [:pair [:number "1"] [:atom [:keyword "Chord"] [:init [:arguments [:string "'G3maj7'"]]]]]]]]]
              want 8]
          (is (= want (get-normalized-total-beats tree)))))))
  ; TODO: "with greater than whole notes"
  (testing "measures"
    (testing "with whole notes"
      (let [tree [:track [:statement [:assign [:identifier ":ABC"] [:list [:pair [:number "1"] [:atom [:keyword "Chord"] [:init [:arguments [:string "'D2min7'"]]]]] [:pair [:number "1"] [:atom [:keyword "Chord"] [:init [:arguments [:string "'G2Maj7'"]]]]] [:pair [:number "1"] [:atom [:keyword "Chord"] [:init [:arguments [:string "'C2maj7'"]]]]]]]] [:statement [:play [:identifier ":ABC"]]]]
            want [[{:duration 1, :notes {:atom {:keyword "Chord", :init {:arguments ["D2min7"]}}}}] [{:duration 1, :notes {:atom {:keyword "Chord", :init {:arguments ["G2Maj7"]}}}}] [{:duration 1, :notes {:atom {:keyword "Chord", :init {:arguments ["C2maj7"]}}}}]]]
        (is (= want (normalize-measures tree))))) )
    (testing "with half notes"
      (let [tree [:track [:statement [:assign [:identifier ":ABC"] [:list [:pair [:div [:number "1"] [:number "2"]] [:atom [:keyword "Chord"] [:init [:arguments [:string "'D2min7'"]]]]] [:pair [:div [:number "1"] [:number "2"]] [:atom [:keyword "Chord"] [:init [:arguments [:string "'G2Maj7'"]]]]] [:pair [:number "1"] [:atom [:keyword "Chord"] [:init [:arguments [:string "'C2maj7'"]]]]]]]] [:statement [:play [:identifier ":ABC"]]]]
            want [[{:duration 1/2, :notes {:atom {:keyword "Chord", :init {:arguments ["D2min7"]}}}} {:duration 1/2, :notes {:atom {:keyword "Chord", :init {:arguments ["G2Maj7"]}}}}] [{:duration 1, :notes {:atom {:keyword "Chord", :init {:arguments ["C2maj7"]}}}} nil]]]
        (is (= want (normalize-measures tree)))))
    (testing "with quarter notes"
      (let [tree [:track [:statement [:assign [:identifier ":ABC"] [:list [:pair [:div [:number "1"] [:number "4"]] [:atom [:keyword "Chord"] [:init [:arguments [:string "'D2min7'"]]]]] [:pair [:div [:number "1"] [:number "2"]] [:atom [:keyword "Chord"] [:init [:arguments [:string "'G2Maj7'"]]]]] [:pair [:div [:number "1"] [:number "4"]] [:atom [:keyword "Chord"] [:init [:arguments [:string "'C2maj7'"]]]]]]]] [:statement [:play [:identifier ":ABC"]]]]
            want [[{:duration 1/4, :notes {:atom {:keyword "Chord", :init {:arguments ["D2min7"]}}}} {:duration 1/2, :notes {:atom {:keyword "Chord", :init {:arguments ["G2Maj7"]}}}} nil {:duration 1/4, :notes {:atom {:keyword "Chord", :init {:arguments ["C2maj7"]}}}}]]]
        (is (= want (normalize-measures tree)))))

  ; FIXME
  ; (testing "with nested measure references (should avoid dupes)"
  ;   (let [tree [:track [:statement [:assign [:identifier ":A"] [:list [:pair [:number "1"] [:atom [:keyword "Scale"] [:init [:arguments [:string "'C2 Major'"]]]]]]]] [:statement [:assign [:identifier ":B"] [:identifier ":A"]]] [:statement [:play [:identifier ":B"]]]]
  ;         want [[{:duration 1, :notes [:atom [:keyword "Scale"] [:init [:arguments [:string "C2 Major"]]]]}]]]
  ;     (is (= want (normalize-measures tree)))))
)

(deftest provisioning
  (testing "headers"
    (testing "static"
      (let [tree [:track [:statement [:header [:meta "Tempo"] [:number "90"]]]]
            want 90]
        (is (= (:tempo (provision-headers tree)) want))))
    (testing "dynamic"
      (testing "total beats"
        (let [tree [:track [:statement [:assign [:identifier ":ABC"] [:list [:pair [:number "1"] [:list]] [:pair [:number "3"] [:list]]]]]]
              want 4]
          (is (= (:total-beats (provision-headers tree)) want))))
      (testing "ms per beat"
        (let [tree [:track [:statement [:assign [:identifier ":ABC"] [:list [:pair [:number "1"] [:list]] [:pair [:number "3"] [:list]]]]]]
              want 2000.0]
          (is (= (:ms-per-beat (provision-headers tree)) want))))
      (testing "lowest beat"
        (let [tree [:track [:statement [:assign [:identifier ":ABC"] [:list [:pair [:div [:number "1"] [:number "4"]] [:list]] [:pair [:number "1"] [:list]]]]]]
              want 1/4]
          (is (= (:lowest-beat (provision-headers tree)) want)))))))

(deftest compilation
  (testing "basic"
    (let [tree [:track [:statement [:assign [:identifier ":ABC"] [:list [:pair [:div [:number "1"] [:number "4"]] [:atom [:keyword "Chord"] [:init [:arguments [:string "'D2min7'"]]]]] [:pair [:div [:number "1"] [:number "2"]] [:atom [:keyword "Chord"] [:init [:arguments [:string "'G2Maj7'"]]]]] [:pair [:div [:number "1"] [:number "4"]] [:atom [:keyword "Chord"] [:init [:arguments [:string "'C2maj7'"]]]]]]]] [:statement [:play [:identifier ":ABC"]]]]
          want {:headers {:title "Untitled", :tempo 120, :time [4 4], :total-beats 1N, :ms-per-beat 500.0, :lowest-beat 1/4, :tags []}, :data [[{:duration 1/4, :notes {:atom {:keyword "Chord", :init {:arguments ["D2min7"]}}}} {:duration 1/2, :notes {:atom {:keyword "Chord", :init {:arguments ["G2Maj7"]}}}} nil {:duration 1/4, :notes {:atom {:keyword "Chord", :init {:arguments ["C2maj7"]}}}}]]}]
      (is (= (compile-track tree) want)))))

