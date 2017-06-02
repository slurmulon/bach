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

(deftest validatation
  ; (testing "assignment"
  ;   (let [want [:track [:statement [:assign [:identifier ":Test"] [:number "1"]]]]]
  ;     (is (= (validate want) true))))
  (testing "identifier (valid, known variable)"
    (let [want [:track [:statement [:assign [:identifier ":A"] [:number "1"]]]
                       [:statement [:assign [:identifier ":B"] [:identifier ":A"]]]]]
      (is (= (validate want) true))))
  (testing "identifier (invalid, unknown variable)"
    (let [want [:track [:statement [:assign [:identifier ":A"] [:number "1"]] [:assign [:identifier "B"] [:identifier "Z"]]]]]
      (is (thrown-with-msg? Exception #"variable is not declared before it's used" (validate want)))))
  (testing "basic div (valid base)"
    (let [want [:track [:statement [:div [:number "1"] [:number "2"]]]]]
      (is (= (validate want) true))))
  (testing "basic div (invalid base)"
    (let [want [:track [:statement [:div [:number "1"] [:number "3"]]]]]
      (is (thrown-with-msg? Exception #"note divisors must be base 2 and no greater than 512" (validate want))))))
