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
  (testing "assignment"
    (let [want [:track [:statement [:assign [:identifier ":Test"] [:number "1"]]]]]
        (is (= (validate want {}) true)))))

