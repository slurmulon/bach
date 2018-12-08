(ns bach.ast-test
  (:require [clojure.test :refer :all]
            [bach.ast :refer :all]
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

; (deftest colors
;   (testing "hex"
;     (let [want [:track [:statement [:color "#FF0000"]]]]
;       (is (= (parse "#FF0000") want)))))

(deftest meta-data
  (testing "tempo"
    (let [want [:track [:statement [:header [:meta "Tempo"] [:number "90"]]]]]
     (is (= want (parse "@Tempo = 90")))))
  (testing "title"
    (let [want [:track [:statement [:header [:meta "Title"] [:string "'Test Track'"]]]]]
      (is (= want (parse "@Title = 'Test Track'")))))
  (testing "time"
    (let [want [:track [:statement [:header [:meta "Time"] [:meter [:number "6"] [:number "8"]]]]]]
      (is (= want (parse "@Time = 6|8")))))
  (testing "tags"
    (let [want [:track [:statement [:header [:meta "Tags"] [:list [:string "'rock'"] [:string "'funk'"]]]]]]
      (is (= want (parse "@Tags = ['rock', 'funk']"))))))

(deftest pair
  (testing "pairs"
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

(deftest atoms ; AKA instantiated keywords
  (testing "note"
    (let [want [:track [:statement [:atom [:keyword "Note"] [:init [:arguments [:string "'C2'"]]]]]]]
      (is (= (parse "Note('C2')") want))))
  (testing "scale"
    (let [want [:track [:statement [:atom [:keyword "Scale"] [:init [:arguments [:string "'C2 Major'"]]]]]]]
      (is (= (parse "Scale('C2 Major')") want))))
  (testing "chord"
    (let [want [:track [:statement [:atom [:keyword "Chord"] [:init [:arguments [:string "'C2maj7'"]]]]]]]
      (is (= (parse "Chord('C2maj7')") want))))
  (testing "multiple arguments"
    (let [want [:track [:statement [:atom [:keyword "Scale"] [:init [:arguments [:string "'C2 Minor'"] [:div [:number "1"] [:number "4"]] [:attribute [:word "color"] [:color "#FF0000"]]]]]]]]
      (is (= (parse "Scale('C2 Minor', 1/4, color: #FF0000)") want)))))
