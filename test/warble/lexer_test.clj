(ns warble.lexer-test
  (:require [clojure.test :refer :all]
            [warble.lexer :refer :all]))

(deftest grammar-test
  (testing "variable can be assigned"
    (let [want [:root [:statement [:assign [:identifier ":Foo "] [:list]]]]]
      (is (= (tokenize ":Foo = []") want)))))