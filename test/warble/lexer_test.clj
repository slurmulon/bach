(ns warble.lexer-test
  (:require [clojure.test :refer :all]
            [warble.lexer :refer :all]
            [instaparse.core :as insta]))

(deftest variables
  (testing "can be assigned"
    (let [want [:track [:statement [:assign [:identifier ":Test"] [:number "1"]]]]]
      (is (= (parse ":Test = 1") want)))))

(deftest terms
  (testing "addition"
    (let [want [:track [:statement [:add [:number "1"] [:number "2"]]]]]
      (is (= (parse "1 + 2") want))))
  (testing "division"
    (let [want [:track [:statement [:div [:number "1"] [:number "2"]]]]]
      (is (= (parse "1 / 2") want))))
  (testing "complex operations"
    (let [want [:track [:statement [:add [:number "1"] [:div [:number "2"] [:number "3"]]]]]]
      (is (= (parse "1 + 2/3") want)))))

(deftest pair
  (testing "term keys"
    (testing "valid"
      (let [want [:track [:statement [:pair [:number "1"] [:list]]]]]
        (is (= (parse "1 -> []") want))))
    (testing "invalid"
      (is (= (insta/failure? (parse "abc -> []")) true)))))

(deftest arrays
  (testing "emptiness"
    (let [want [:track [:statement [:list]]]]
      (is (= (parse "[]") want))))
  (testing "multiple elements"
    (let [want [:track [:statement [:list [:number "1"] [:number "2"]]]]]
      (is (= (parse "[1, 2]") want))))
  (testing "can contain pairs"
    (let [want [:track [:statement [:list [:pair [:number "1"] [:identifier ":Foo"]] [:pair [:number "2"] [:identifier ":Bar"]]]]]]
      (is (= (parse "[1 -> :Foo, 2 -> :Bar]") want))))
  (testing "can be nested"
    (let [want [:track [:statement [:list [:list]]]]]
      (is (= (parse "[[]]")) want)))
  (testing "can contain nested tuples"
    (let [want [:track [:statement [:list [:pair [:number "1"] [:list]]]]]]
      (is (= (parse "[1 -> []]")) want))))
