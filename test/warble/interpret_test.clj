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

(deftest denormalization
  (testing "variables - dynamic value"
    (let [tree [:track [:statement [:assign [:identifier :A] [:number 1]]]
                       [:statement [:assign [:identifier :B] [:identifier :A]]]]
          want [:track [:statement [:assign [:identifier :A] [:number 1]]]
                       [:statement [:assign [:identifier :B] [:number 1]]]]]
      (is (= want (denormalize-variables tree))))))