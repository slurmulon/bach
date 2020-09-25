(ns bach.ast-test
  (:require [clojure.test :refer :all]
            [bach.ast :refer :all]
            [instaparse.core :as insta]))

(deftest variables
  (testing "can be assigned"
    (let [want [:track
                [:statement
                 [:assign [:identifier ":Test"] [:number "1"]]]]]
      (is (= want (parse ":Test = 1"))))))

(deftest primitives
  (testing "integer"
    (let [want [:track
                [:statement
                 [:number "1"]]]]
      (is (= want (parse "1")))))
  (testing "double"
    (let [want [:track
                [:statement
                 [:number "2.5"]]]]
      (is (= want (parse "2.5"))))))

(deftest terms
  (testing "addition"
    (let [want [:track
                [:statement
                 [:add [:number "1"] [:number "2"]]]]]
      (is (= want (parse "1 + 2")))))
  (testing "division"
    (let [want [:track
                [:statement
                 [:div [:number "1"] [:number "2"]]]]]
      (is (= want (parse "1 / 2")))))
  (testing "complex operations"
    (testing "whole number plus rational number"
      (let [want [:track
                  [:statement
                   [:add
                    [:number "1"]
                    [:div [:number "2"] [:number "3"]]]]]]
        (is (= want (parse "1 + 2/3")))))
    (testing "rational number plus rational number"
      (let [want [:track
                  [:statement
                   [:add
                    [:div [:number "1"] [:number "4"]]
                    [:div [:number "1"] [:number "8"]]]]]]
        (is (= want (parse "1/4 + 1/8"))))))
  (testing "parenthesized operations"
    (testing "multiplication and addition"
      (let [want [:track
                  [:statement
                   [:mul
                    [:number "2"]
                    [:add [:number "1"] [:number "3"]]]]]]
        (is (= want (parse "2 * (1 + 3)")))))
    (testing "multiplication and division"
      (let [want [:track
                  [:statement
                   [:mul
                    [:number "2"]
                    [:div [:number "6"] [:number "8"]]]]]]
        (is (= want (parse "2 * (6 / 8)")))))))

(deftest colors
  (testing "hex"
    (testing "3 digits"
      (let [want [:track
                  [:statement
                   [:color "#000"]]]]
        (is (= want (parse "#000")))))
    (testing "6 digits"
      (let [want [:track
                  [:statement
                   [:color "#FF0000"]]]]
        (is (= want (parse "#FF0000")))))))

(deftest meta-data
  (testing "tempo"
    (let [want [:track
                [:statement
                 [:header
                  [:meta "Tempo"]
                  [:number "90"]]]]]
      (is (= want (parse "@Tempo = 90")))))
  (testing "title"
    (let [want [:track
                [:statement
                 [:header
                  [:meta "Title"]
                  [:string "'Test Track'"]]]]]
      (is (= want (parse "@Title = 'Test Track'")))))
  (testing "time"
    (let [want [:track
                [:statement
                 [:header
                  [:meta "Time"]
                  [:meter [:number "6"] [:number "8"]]]]]]
      (is (= want (parse "@Time = 6|8")))))
  (testing "tags"
    (let [want [:track
                [:statement
                 [:header
                  [:meta "Tags"]
                  [:list [:string "'rock'"] [:string "'funk'"]]]]]]
      (is (= want (parse "@Tags = ['rock', 'funk']"))))))

(deftest pairs
  (testing "valid"
    (testing "basic"
      (let [want [:track
                  [:statement
                   [:pair [:number "1"] [:list]]]]]
        (is (= want (parse "1 -> []")))))
    (testing "expression"
      (testing "basic"
        (let [want [:track
                    [:statement
                     [:pair
                      [:mul
                       [:number "4"]
                       [:div [:number "6"] [:number "8"]]]
                      [:list]]]]]
          (is (= want (parse "4 * 6/8 -> []")))))
      (testing "nested"
        (let [want [:track
                    [:statement
                     [:list
                      [:pair
                       [:mul
                        [:number "4"]
                        [:div [:number "6"] [:number "8"]]]
                       [:list]]]]]]
          (is (= want (parse "[4 * 6/8 -> []]")))))
      (testing "optional parens"
        (let [want [:track
                    [:statement
                     [:list
                      [:pair
                       [:mul
                        [:number "4"]
                        [:div [:number "6"] [:number "8"]]]
                       [:list]]]]]]
          (is (= want (parse "[(4 * 6/8) -> []]")))))
      (testing "line breaks"
        (let [want [:track
                    [:statement
                     [:list
                      [:pair
                       [:mul
                        [:number "4"]
                        [:div [:number "6"] [:number "8"]]]
                       [:list]]]]]]
          (is (= want (parse "[\n4 * 6/8 -> []\n]")))))
      (testing "arbitrary whitespace"
        (let [want [:track
                    [:statement
                     [:list
                      [:pair
                       [:mul
                        [:number "4"]
                        [:div [:number "6"] [:number "8"]]]
                       [:list]]]]]]
          (is (= want (parse "[\n  4 * 6/8 -> []\n]")))))))
  ; TODO: sequential pairs
  (testing "invalid"
    (testing "identifier as key"
      (is (= true (insta/failure? (parse "abc -> []")))))
    (testing "missing value"
      (is (= true (insta/failure? (parse "abc ->")))))))

(deftest arrays
  (testing "emptiness"
    (let [want [:track [:statement [:list]]]]
      (is (= want (parse "[]")))))
  (testing "multiple elements"
    (let [want [:track
                [:statement
                 [:list [:number "1"] [:number "2"]]]]]
      (is (= want (parse "[1, 2]")))))
  (testing "can contain pairs"
    (let [want [:track
                [:statement
                 [:list
                  [:pair
                   [:number "1"]
                   [:identifier ":Foo"]]
                  [:pair
                   [:number "2"]
                   [:identifier ":Bar"]]]]]]
      (is (= want (parse "[1 -> :Foo, 2 -> :Bar]")))))
  ; TODO: Ensure nesting of lists can only be 1 level deep!
  (testing "can be nested"
    (let [want [:track
                [:statement
                 [:list [:list]]]]]
      (is (= want (parse "[[]]")))))
  (testing "can contain nested tuples"
    (let [want [:track
                [:statement
                 [:list
                  [:pair
                   [:number "1"]
                   [:list]]]]]]
      (is (= want (parse "[1 -> []]"))))))

(deftest atoms ; AKA instantiated keywords
  (testing "note"
    (let [want [:track
                [:statement
                 [:atom
                  [:keyword "Note"]
                  [:arguments [:string "'C2'"]]]]]]
      (is (= want (parse "Note('C2')")))))
  (testing "scale"
    (let [want [:track
                [:statement
                 [:atom
                  [:keyword "Scale"]
                  [:arguments [:string "'C2 Major'"]]]]]]
      (is (= want (parse "Scale('C2 Major')")))))
  (testing "multiple arguments"
    (let [want [:track
                [:statement
                 [:atom
                  [:keyword "Scale"]
                  [:arguments
                   [:string "'C2 Minor'"]
                   [:div
                    [:number "1"]
                    [:number "4"]]
                   [:attribute
                    [:word "color"]
                    [:color "#FF0000"]]]]]]]
      (is (= want (parse "Scale('C2 Minor', 1/4, color: #FF0000)"))))))
