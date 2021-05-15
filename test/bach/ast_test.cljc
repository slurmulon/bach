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
      (testing "identifier"
        (let [want [:track
                    [:statement
                     [:beat
                      [:mul
                       [:int "4"]
                       [:div [:int "6"] [:int "8"]]]
                      [:identifier [:name "a"]]]]]]
          (is (= want (parse "4 * 6/8 -> :a")))))
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

(deftest whens
  (testing "matchers"
    (testing "even?"
      (let [code "10 of [ :a, when even? do :b ]"
            want [:track
                  [:statement
                   [:loop
                    [:int "10"]
                    [:list
                     [:identifier [:name "a"]]
                     [:when
                      [:when-match "even"]
                      [:identifier [:name "b"]]]]]]]]
        (is (= want (parse code)))))
   (testing "odd?"
      (let [code "10 of [ :a, when odd? do :b ]"
            want [:track
                  [:statement
                   [:loop
                    [:int "10"]
                    [:list
                     [:identifier [:name "a"]]
                     [:when
                      [:when-match "odd"]
                      [:identifier [:name "b"]]]]]]]]
        (is (= want (parse code)))))
    (testing "last?"
      (let [code "10 of [ :a, when last? do :b ]"
            want [:track
                  [:statement
                   [:loop
                    [:int "10"]
                    [:list
                     [:identifier [:name "a"]]
                     [:when
                      [:when-match "last"]
                      [:identifier [:name "b"]]]]]]]]
        (is (= want (parse code)))))
   (testing "first?"
      (let [code "10 of [ :a, when first? do :b ]"
            want [:track
                  [:statement
                   [:loop
                    [:int "10"]
                    [:list
                     [:identifier [:name "a"]]
                     [:when
                      [:when-match "first"]
                      [:identifier [:name "b"]]]]]]]]
        (is (= want (parse code))))))
  (testing "comparisons"
    (testing "gte?"
      (let [code "10 of [ :a, when gte? 4 do :b ]"
            want [:track
                  [:statement
                   [:loop
                    [:int "10"]
                    [:list
                     [:identifier [:name "a"]]
                     [:when
                      [:when-comp "gte" [:int "4"]]
                      [:identifier [:name "b"]]]]]]]]
        (is (= want (parse code)))))
    (testing "gt?"
      (let [code "10 of [ :a, when gt? 3 do :b ]"
            want [:track
                  [:statement
                   [:loop
                    [:int "10"]
                    [:list
                     [:identifier [:name "a"]]
                     [:when
                      [:when-comp "gt" [:int "3"]]
                      [:identifier [:name "b"]]]]]]]]
        (is (= want (parse code)))))
    (testing "lte?"
      (let [code "12 of [ :a, when lte? 7 do :b ]"
            want [:track
                  [:statement
                   [:loop
                    [:int "12"]
                    [:list
                     [:identifier [:name "a"]]
                     [:when
                      [:when-comp "lte" [:int "7"]]
                      [:identifier [:name "b"]]]]]]]]
        (is (= want (parse code)))))
    (testing "lt?"
      (let [code "12 of [ :a, when lt? 7 do :b ]"
            want [:track
                  [:statement
                   [:loop
                    [:int "12"]
                    [:list
                     [:identifier [:name "a"]]
                     [:when
                      [:when-comp "lt" [:int "7"]]
                      [:identifier [:name "b"]]]]]]]]
        (is (= want (parse code)))))
    (testing "whitespace"
      (let [code "12 of [ :a, when lt? 7 do :b :c :d ]"
            want [:track
                  [:statement
                   [:loop
                    [:int "12"]
                    [:list
                     [:identifier [:name "a"]]
                     [:when
                      [:when-comp "lt" [:int "7"]]
                      [:identifier [:name "b"]]]
                     [:identifier [:name "c"]]
                     [:identifier [:name "d"]]]]]]]
        (is (= want (parse code))))))
  (testing "conditions"
    (testing "int"
      (let [code "2 of [ when 1 do :a, when 2 do :b ]"
            want [:track
                  [:statement
                   [:loop
                    [:int "2"]
                    [:list
                     [:when
                      [:int "1"]
                      [:identifier [:name "a"]]]
                     [:when
                      [:int "2"]
                      [:identifier [:name "b"]]]]]]]]
        (is (= want (parse code)))))
    (testing "range"
      (let [code "5 of [ when 1..3 do :a, when 4..5 do :b ]"
            want [:track
                  [:statement
                   [:loop
                    [:int "5"]
                    [:list
                     [:when
                      [:range [:int "1"] [:int "3"]]
                      [:identifier [:name "a"]]]
                     [:when
                      [:range [:int "4"] [:int "5"]]
                      [:identifier [:name "b"]]]]]]]]
        (is (= want (parse code)))))
    (testing "expressions"
      (testing "all"
        (let [code "5 of [ when [1 5] do :a, when [2 4] do :b ]"
              want [:track
                    [:statement
                     [:loop
                      [:int "5"]
                      [:list
                       [:when
                        [:when-all [:int "1"] [:int "5"]]
                        [:identifier [:name "a"]]]
                       [:when
                        [:when-all [:int "2"] [:int "4"]]
                        [:identifier [:name "b"]]]]]]]]
          (is (= want (parse code)))))
      (testing "any"
       (let [code "5 of [ when {1 3 5} do :a, when {2 4} do :b ]"
             want [:track
                   [:statement
                    [:loop
                     [:int "5"]
                     [:list
                       [:when
                        [:when-any [:int "1"] [:int "3"] [:int "5"]]
                        [:identifier [:name "a"]]]
                       [:when
                        [:when-any [:int "2"] [:int "4"]]
                        [:identifier [:name "b"]]]]]]]]
          (is (= want (parse code)))))
      (testing "not"
        (testing "all"
          (let [code "10 of [ when ![even? 1..5] do :x ]"
                want [:track
                      [:statement
                       [:loop
                        [:int "10"]
                        [:list
                         [:when
                          [:when-not
                           [:when-all
                            [:when-match "even"]
                            [:range
                             [:int "1"]
                             [:int "5"]]]]
                          [:identifier [:name "x"]]]]]]]]
            (is (= want (parse code)))))
        (testing "any"
          (let [code "10 of [ when !{even? 1..5} do :x ]"
                want [:track
                      [:statement
                       [:loop
                        [:int "10"]
                        [:list
                         [:when
                          [:when-not
                           [:when-any
                            [:when-match "even"]
                            [:range
                             [:int "1"]
                             [:int "5"]]]]
                          [:identifier [:name "x"]]]]]]]]
            (is (= want (parse code))))))
      (testing "nested"
        (testing "any -> all"
          (let [code "10 of [ when { 1 3 [ 5..10 even? ] } do :x ]"
                want [:track
                      [:statement
                       [:loop
                        [:int "10"]
                        [:list
                         [:when
                          [:when-any
                           [:int "1"]
                           [:int "3"]
                           [:when-all
                            [:range [:int "5"] [:int "10"]]
                            [:when-match "even"]]]
                          [:identifier [:name "x"]]]]]]]]
            (is (= want (parse code)))))))
    (testing "matches"
      (testing "even")
      (testing "odd")
      (testing "last")
      (testing "first"))))

(deftest comments
  (testing "basic"
    (let [code "## Hello"
          want [:track [:statement]]]
      (is (= want (parse code)))))
  (testing "nested"
    (testing "in list"
      (let [code "[ 1 -> Chord('A')\n## Ignore\n 2 -> Chord('B') ]"
            want [:track
                  [:statement
                   [:list
                    [:beat
                     [:int "1"]
                     [:atom
                      [:keyword [:name "Chord"]]
                      [:arguments [:string "'A'"]]]]
                    [:beat
                     [:int "2"]
                     [:atom
                      [:keyword [:name "Chord"]]
                      [:arguments [:string "'B'"]]]]]]]]
        (is (= want (parse code)))))
    (testing "in set"
      (let [code "{ 1 -> Chord('A')\n## Ignore\n 2 -> Chord('B') }"
            want [:track
                  [:statement
                   [:set
                    [:beat
                     [:int "1"]
                     [:atom
                      [:keyword [:name "Chord"]]
                      [:arguments [:string "'A'"]]]]
                    [:beat
                     [:int "2"]
                     [:atom
                      [:keyword [:name "Chord"]]
                      [:arguments [:string "'B'"]]]]]]]]
        (is (= want (parse code)))))))
