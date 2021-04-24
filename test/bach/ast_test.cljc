(ns bach.ast-test
  (:require #?@(:clj [[clojure.test :refer [deftest is testing]]]
               :cljs [[cljs.test :refer-macros [deftest is testing run-tests]]
                      [goog.string :as gstring]
                      [goog.string.format :as format]])
            [bach.ast :refer [parse]]
            [instaparse.core :as insta]))

(deftest variables
  (testing "can be assigned"
    (let [want [:track
                [:statement
                 [:assign
                  [:identifier
                   [:name "Test"]]
                  [:int "1"]]]]]
      (is (= want (parse ":Test = 1")))))
  (testing "name bindings"
    (testing "must be at least one characer"
      (is (= true (insta/failure? (parse ": = 1")))))
    (testing "leading character"
      (testing "allowed"
        (testing "alphabet"
          (for [var-name (clojure.string/split "abcdefghijklmnopqrstuvwyxz_" #"")
                bach-source (->> var-name #(#?(:clj format :cljs gstring/format) ":%s = 4" %))]
            (let [want [:track
                        [:statement
                         [:assign
                          [:identifier
                           [:name var-name]]
                          [:int "1"]]]]
                  data (->> var-name #(#?(:clj format :cljs gstring/format) ":%s = 4" %))]
              (is (= want (parse data)))))))
      (testing "disallowed"
        (for [var-name (clojure.string/split "01234567890`~!@#$%^&*(){}[]<>?:;/\\'\"" #"")
              bach-source (->> var-name #(#?(:clj format :cljs gstring/format) ":%s = 1" %))]
          (is (= true (insta/failure? (parse bach-source)))))))))

(deftest primitives
  (testing "integer"
    (let [want [:track
                [:statement
                 [:int "1"]]]]
      (is (= want (parse "1")))))
  (testing "float"
    (let [want [:track
                [:statement
                 [:float "2.5"]]]]
      (is (= want (parse "2.5"))))))

(deftest terms
  (testing "addition"
    (let [want [:track
                [:statement
                 [:add [:int "1"] [:int "2"]]]]]
      (is (= want (parse "1 + 2")))))
  (testing "division"
    (let [want [:track
                [:statement
                 [:div [:int "1"] [:int "2"]]]]]
      (is (= want (parse "1 / 2")))))
  (testing "complex operations"
    (testing "whole number plus rational number"
      (let [want [:track
                  [:statement
                   [:add
                    [:int "1"]
                    [:div [:int "2"] [:int "3"]]]]]]
        (is (= want (parse "1 + 2/3")))))
    (testing "rational number plus rational number"
      (let [want [:track
                  [:statement
                   [:add
                    [:div [:int "1"] [:int "4"]]
                    [:div [:int "1"] [:int "8"]]]]]]
        (is (= want (parse "1/4 + 1/8"))))))
  (testing "parenthesized operations"
    (testing "multiplication and addition"
      (let [want [:track
                  [:statement
                   [:mul
                    [:int "2"]
                    [:add [:int "1"] [:int "3"]]]]]]
        (is (= want (parse "2 * (1 + 3)")))))
    (testing "multiplication and division"
      (let [want [:track
                  [:statement
                   [:mul
                    [:int "2"]
                    [:div [:int "6"] [:int "8"]]]]]]
        (is (= want (parse "2 * (6 / 8)")))))))

(deftest headers
  (testing "tempo"
    (let [want [:track
                [:statement
                 [:header
                  [:meta [:name "Tempo"]]
                  [:int "90"]]]]]
      (is (= want (parse "@Tempo = 90")))))
  (testing "meter"
    (let [want [:track
                [:statement
                 [:header
                  [:meta [:name "Meter"]]
                  [:meter [:int "6"] [:int "8"]]]]]]
      (is (= want (parse "@Meter = 6|8")))))
 (testing "custom"
    (let [want [:track
                [:statement
                 [:header
                  [:meta [:name "Title"]]
                  [:string "'Test Track'"]]]]]
      (is (= want (parse "@Title = 'Test Track'"))))))

(deftest beats
  (testing "valid"
    (testing "basic"
      (let [want [:track
                  [:statement
                   [:beat [:int "1"] [:set]]]]]
        (is (= want (parse "1 -> {}")))))
    (testing "expression"
      (testing "basic"
        (let [want [:track
                    [:statement
                     [:beat
                      [:mul
                       [:int "4"]
                       [:div [:int "6"] [:int "8"]]]
                      [:set]]]]]
          (is (= want (parse "4 * 6/8 -> {}")))))
      (testing "nested"
        (let [want [:track
                    [:statement
                     [:list
                      [:beat
                       [:mul
                        [:int "4"]
                        [:div [:int "6"] [:int "8"]]]
                       [:set]]]]]]
          (is (= want (parse "[4 * 6/8 -> {}]")))))
      (testing "optional parens"
        (let [want [:track
                    [:statement
                     [:list
                      [:beat
                       [:mul
                        [:int "4"]
                        [:div [:int "6"] [:int "8"]]]
                       [:set]]]]]]
          (is (= want (parse "[(4 * 6/8) -> {}]")))))
      (testing "line breaks"
        (let [want [:track
                    [:statement
                     [:list
                      [:beat
                       [:mul
                        [:int "4"]
                        [:div [:int "6"] [:int "8"]]]
                       [:set]]]]]]
          (is (= want (parse "[\n4 * 6/8 -> {}\n]")))))
      (testing "arbitrary whitespace"
        (let [want [:track
                    [:statement
                     [:list
                      [:beat
                       [:mul
                        [:int "4"]
                        [:div [:int "6"] [:int "8"]]]
                       [:set]]]]]]
          (is (= want (parse "[\n  4 * 6/8 -> {}\n]")))))))
  ; TODO: sequential beats
  (testing "invalid"
    (testing "identifier as key"
      (is (= true (insta/failure? (parse "abc -> {}")))))
    (testing "missing value"
      (is (= true (insta/failure? (parse "abc ->")))))))

(deftest lists
  (testing "emptiness"
    (let [want [:track [:statement [:list]]]]
      (is (= want (parse "[]")))))
  (testing "multiple elements"
    (let [want [:track
                [:statement
                 [:list
                  [:identifier [:name "a"]]
                  [:identifier [:name "b"]]]]]]
      (is (= want (parse "[:a, :b]")))))
  (testing "can contain beats"
    (let [want [:track
                [:statement
                 [:list
                  [:beat
                   [:int "1"]
                   [:identifier [:name "Foo"]]]
                  [:beat
                   [:int "2"]
                   [:identifier [:name "Bar"]]]]]]]
      (is (= want (parse "[1 -> :Foo, 2 -> :Bar]")))))
  (testing "can be nested"
    (let [want [:track
                [:statement
                 [:list [:list]]]]]
      (is (= want (parse "[[]]")))))
  (testing "can contain nested tuples"
    (let [want [:track
                [:statement
                 [:list
                  [:beat
                   [:int "1"]
                   [:set]]]]]]
      (is (= want (parse "[1 -> {}]"))))))

(deftest atoms ; AKA instantiated keywords
  (testing "note"
    (let [want [:track
                [:statement
                 [:atom
                  [:keyword [:name "Note"]]
                  [:arguments [:string "'C2'"]]]]]]
      (is (= want (parse "Note('C2')")))))
  (testing "scale"
    (let [want [:track
                [:statement
                 [:atom
                  [:keyword [:name "Scale"]]
                  [:arguments [:string "'C2 Major'"]]]]]]
      (is (= want (parse "Scale('C2 Major')")))))
  (testing "multiple arguments"
    (let [want [:track
                [:statement
                 [:atom
                  [:keyword [:name "Scale"]]
                  [:arguments
                   [:string "'C2 Minor'"]
                   [:div
                    [:int "1"]
                    [:int "4"]]
                   [:attribute
                    [:name "color"]
                    [:string "'#FF0000'"]]]]]]]
      (is (= want (parse "Scale('C2 Minor', 1/4, color: '#FF0000')"))))))
