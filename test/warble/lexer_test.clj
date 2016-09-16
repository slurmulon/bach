(ns warble.lexer-test
  (:require [clojure.test :refer :all]
            [warble.lexer :refer :all]))

(deftest grammar-test
  (testing "variables"
    (testing "can be assigned"
      (let [want [:root [:statement [:assign [:identifier ":Test "] [:list]]]]]
        (is (= (tokenize ":Test = []") want)))))
  (testing "terms"
    (testing "addition"
      (let [want [:root [:statement [:add [:number "1"] [:number "2"]]]]]
        (is (= (tokenize "1 + 2") want))))
    (testing "division"
      (let [want [:root [:statement [:div [:number "1"] [:number "2"]]]]]
        (is (= (tokenize "1 / 2") want))))
    (testing "complex operations"
      (let [want [:root [:statement [:add [:number "1"] [:div [:number "2"] [:number "3"]]]]]]
        (is (= (tokenize "1 + 2/3") want))))))
