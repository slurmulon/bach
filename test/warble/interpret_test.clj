(ns warble.interpret-test
  (:require [clojure.test :refer :all]
            [warble.lexer :refer :all]
            [warble.interpret :refer :all]))

(deftest defaults
  (testing "tempo"
    (is (= 120 default-tempo)))
  (testing "time-signature"
    (is (= 1 default-time-signature)))
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

(deftest conversion
  (testing "number"
    (let [tree [:track [:statement [:div [:number "1"] [:number "4"]]]]
          want [:track [:statement [:div [:number 1] [:number 4]]]]]
      (is (= want (convert-values tree))))))

; (deftest lowest-beat
;   (testing "finds the lowest beat amongst all of the pairs"
;     (let [tree [:track [:statement [:list [:pair [:number "4"]] [:pair [:number "2"]
;     )

; FIXME: nested transitive variables are broken (:A = 1, :B = :A, :C = :B)
(deftest dereferencing
  (testing "variables (simple)"
    (let [tree [:track [:statement [:assign [:identifier :A] [:number 1]]]
                       [:statement [:assign [:identifier :B] [:identifier :A]]]]
          want [:track [:statement [:assign [:identifier :A] [:number 1]]]
                       [:statement [:assign [:identifier :B] [:number 1]]]]]
      (is (= want (dereference-variables tree)))))
  ; (testing "variables (simple)"
  ;   (let [tree [:track [:statement [:assign [:identifier :A] [:number 1]]]
  ;                      [:statement [:assign [:identifier :B] [:identifier :A]]]
  ;                      [:statement [:assign [:identifier :C] [:identifier :B]]]]
  ;         want [:track [:statement [:assign [:identifier :A] [:number 1]]]
  ;                      [:statement [:assign [:identifier :B] [:number 1]]]
  ;                      [:statement [:assign [:identifier :C] [:number 1]]]]]
  ;     (is (= want (denormalize-variables tree))))))
  (testing "beats"
    (let [tree [:track [:statement [:assign [:identifier ":ABC"] [:list [:pair [:number "1"] [:atom [:keyword "Chord"] [:init [:arguments [:string "'D2min7'"]]]]] [:pair [:number "3"] [:atom [:keyword "Chord"] [:init [:arguments [:string "'B2Maj7'"]]]]]]]]]
          want [:track [:statement [:assign [:identifier ":ABC"] [:list [:beat [:list [:atom [:keyword "Chord"] [:init [:arguments [:string "'D2min7'"]]]]]] [:beat []] [:beat [:atom [:keyword "Chord"] [:init [:arguments [:string "'B2Maj7'"]]]]]]]]]]
      (is (= true true)))))
