(ns ^:eftest/synchronized bach.track-test
  (:require #?@(:clj [[clojure.test :refer [deftest is testing]]]
               :cljs [[bach.crypto]
                      [cljs.test :refer-macros [deftest is testing run-tests]]
                      [goog.string :as gstring]
                      [goog.string.format]])
            [instaparse.core :as insta]
            [bach.track :as track]
            [bach.data :as data]))

(deftest defaults
  (testing "tempo"
    (= track/default-tempo 120))
  (testing "meter"
    (= track/default-meter [4 4]))
  (testing "headers"
    (= track/default-headers {:tempo 120 :meter [4 4]})))

(deftest headers
  (testing "get-headers"
    ; case insensitive
    (testing "reserved"
      (let [tree [:track
                   [:statement
                    [:header [:meta [:name "tempo"]] [:number "90"]]
                    [:header [:meta [:name "meter"]] [:meter [:int "3"] [:int "4"]]]]]
            want {:tempo 90 :meter [3 4]}
            actual (track/get-headers tree)]
        (= want actual)))
    (testing "custom"
      (let [tree [:track
                   [:statement
                    [:header [:meta [:name "title"]] [:number "90"]]
                    [:header [:meta [:name "octave"]] [:number "2"]]]]
            want {:title 90 :octave 2}
            actual (track/get-headers tree)]
        (= want actual)))))

(deftest tempo
  (testing "get-tempo"
    (testing "basic"
      (let [tree [:track
                  [:statement
                   [:header [:meta [:name "tempo"]] [:number "90"]]]]
            want 90
            actual (track/get-tempo tree)]
    (testing "case insensitive"
      (let [tree [:track
                  [:statement
                   [:header [:meta [:name "tEmPo"]] [:number "90"]]]]
            want 90
            actual (track/get-tempo tree)]
        (= want actual)))
    (testing "returns default if none provided"
      (let [want 120
            actual (track/get-tempo [])]
        (= want actual)))))))

(deftest meter
  (testing "get-meter"
    (testing "basic"
      (let [tree [:track
                  [:statement
                   [:header [:meta [:name "meter"]] [:meter [:int "3"] [:int "4"]]]]]
            want [3 4]
            actual (track/get-tempo tree)]
    (testing "case insensitive"
      (let [tree [:track
                  [:statement
                   [:header [:meta [:name "MeTeR"]] [:meter [:int "3"] [:int "4"]]]]]
            want [3 4]
            actual (track/get-tempo tree)]
        (= want actual)))
    (testing "returns default if none provided"
      (let [want [4 4]
            actual (track/get-tempo [])]
        (= want actual))))))
  (testing "meter-as-ratio"
    (= 1 (track/meter-as-ratio [4 4]))
    (= (/ 1 2) (track/meter-as-ratio [2 4]))
    (= (/ 3 4) (track/meter-as-ratio [6 8]))
    (= (/ 5 8) (track/meter-as-ratio [5 8]))
    (= (/ 12 8) (track/meter-as-ratio [12 8])))
  (testing "get-meter-ratio"
    (testing "basic ratio"
      (let [tree [:track
                  [:statement
                    [:header [:meta [:name "meter"]] [:meter [:int "6"] [:int "8"]]]]]]
        (= (/ 3 4) (track/get-meter-ratio tree))))
    (testing "irrational ratio"
      (let [tree [:track
                  [:statement
                    [:header [:meta [:name "meter"]] [:meter [:int "0"] [:int "1"]]]]]]
        (= 1 (track/get-meter-ratio tree))))))

(deftest variables
  (testing "resolve-variables"
    (testing "assign")
    (testing "identifier")
    (testing "play")))

(deftest resolution)
(deftest validation)
(deftest pulse-beat)
(deftest step-beat)
