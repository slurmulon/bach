(ns warble.lexer-test
  (:require [clojure.test :refer :all]
            [warble.lexer :refer :all]))

(deftest variables
  (testing "can be assigned"
    (let [want [:root [:statement [:assign [:identifier ":Test"] [:number "1"]]]]]
      (is (= (tokenize ":Test = 1") want)))))

(deftest terms
  (testing "addition"
    (let [want [:root [:statement [:add [:number "1"] [:number "2"]]]]]
      (is (= (tokenize "1 + 2") want))))
  (testing "division"
    (let [want [:root [:statement [:div [:number "1"] [:number "2"]]]]]
      (is (= (tokenize "1 / 2") want))))
  (testing "complex operations"
    (let [want [:root [:statement [:add [:number "1"] [:div [:number "2"] [:number "3"]]]]]]
      (is (= (tokenize "1 + 2/3") want)))))

(deftest list
  (testing "emptiness"
    (let [want [:root [:statement [:list]]]]
      (is (= (tokenize "[]") want))))
  (testing "multiple elements"
    (let [want [:root [:statement [:list [:number "1"] [:number "2"]]]]]
      (is (= (tokenize "[1, 2]") want))))
  (testing "can contain pairs"
    (let [want [:root [:statement [:list [:pair [:number "1"] [:identifier ":Foo"]] [:pair [:number "2"] [:identifier ":Bar"]]]]]]
      (is (= (tokenize "[1 -> :Foo, 2 -> :Bar]") want)))))
