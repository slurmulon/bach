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
    (is (= track/default-tempo 120)))
  (testing "meter"
    (is (= track/default-meter [4 4])))
  (testing "headers"
    (is (= track/default-headers {:tempo 120 :meter [4 4]}))))

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
        (is (= want actual))))
    (testing "custom"
      (let [tree [:track
                   [:statement
                    [:header [:meta [:name "title"]] [:number "90"]]
                    [:header [:meta [:name "octave"]] [:number "2"]]]]
            want {:meter [4 4]
                  :tempo 120
                  :title 90
                  :octave 2}
            actual (track/get-headers tree)]
        (is (= want actual))))))

(deftest tempo
  (testing "get-tempo"
    (testing "basic"
      (let [tree [:track
                  [:statement
                   [:header [:meta [:name "tempo"]] [:number "90"]]]]
            want 90
            actual (track/get-tempo tree)]
        (is (= want actual))))
    (testing "case insensitive"
      (let [tree [:track
                  [:statement
                   [:header [:meta [:name "tEmPo"]] [:number "90"]]]]
            want 90
            actual (track/get-tempo tree)]
        (is (= want actual))))
    (testing "returns default if none provided"
      (let [want 120
            actual (track/get-tempo [])]
        (is (= want actual))))))

(deftest meter
  (testing "get-meter"
    (testing "basic"
      (let [tree [:track
                  [:statement
                   [:header [:meta [:name "meter"]] [:meter [:int "3"] [:int "4"]]]]]
            want [3 4]
            actual (track/get-meter tree)]
        (is (= want actual))))
    (testing "case insensitive"
      (let [tree [:track
                  [:statement
                   [:header [:meta [:name "MeTeR"]] [:meter [:int "3"] [:int "4"]]]]]
            want [3 4]
            actual (track/get-meter tree)]
        (is (= want actual))))
    (testing "returns default if none provided"
      (let [want [4 4]
            actual (track/get-meter [])]
        (is (= want actual)))))
  (testing "meter-as-ratio"
    (is (= 1 (track/meter-as-ratio [4 4])))
    (is (= (/ 1 2) (track/meter-as-ratio [2 4])))
    (is (= (/ 3 4) (track/meter-as-ratio [6 8])))
    (is (= (/ 5 8) (track/meter-as-ratio [5 8])))
    (is (= (/ 12 8) (track/meter-as-ratio [12 8]))))
  (testing "get-meter-ratio"
    (testing "basic ratio"
      (let [tree [:track
                  [:statement
                   [:header [:meta [:name "meter"]] [:meter [:int "6"] [:int "8"]]]]]]
        (is (= (/ 3 4) (track/get-meter-ratio tree)))))
    (testing "irrational ratio"
      (let [tree [:track
                  [:statement
                   [:header [:meta [:name "meter"]] [:meter [:int "0"] [:int "1"]]]]]]
        (is (= 0 (track/get-meter-ratio tree)))))))

(deftest resolve-variables
  (testing "assign"
    (testing "binds value to name"
      (let [tree [:track
                  [:statement
                    [:assign
                      [:identifier :a]
                      [:string "'foo'"]]
                    [:assign
                      [:identifier :b]
                      [:identifier :a]]]]
            want [:track
                  [:statement
                    [:assign
                      [:identifier :a]
                      [:string "'foo'"]]
                    [:assign
                      [:identifier :b]
                      [:string "'foo'"]]]]]
        (is (= want (track/resolve-variables tree))))))
  (testing "identifier")
  (testing "play"))

(deftest resolve-values
  (testing "number"
    (is (= 4 (track/resolve-values [:number "4"])))
    (is (= 5.25 (track/resolve-values [:number "5.25"]))))
  (testing "int"
    (is (= 8 (track/resolve-values [:number "8"]))))
  (testing "float"
    (is (= 12.34567890 (track/resolve-values [:number "12.34567890"]))))
  (testing "+"
    (is (= 9 (track/resolve-values [:add [:number "1"] [:number "8"]]))))
  (testing "-")
    (is (= 3 (track/resolve-values [:sub [:number "7"] [:number "4"]]))))
  (testing "*"
    (is (= 12 (track/resolve-values [:mul [:number "3"] [:number "4"]]))))
  (testing "/"
    (is (= 5 (track/resolve-values [:div [:number "10"] [:number "2"]]))))
  (testing "expr"
    (is (= (/ 3 4) (track/resolve-values
                     [:add
                       [:div [:number "1"] [:number "2"]]
                       [:div [:number "1"] [:number "4"]]]))))
  (testing "meter"
    (is (= [5 8] (track/resolve-values [:meter [:int "5"] [:int "8"]]))))
  (testing "name"
    (is (= 'foo (track/resolve-values [:name "foo"]))))
  (testing "string"
    (is (= "hello bach" (track/resolve-values [:string "'hello bach'"]))))

(deftest resolve-durations
  (testing "dynamic"
    (testing "beat"
      (let [tree [:track
                  [:statement
                   [:header
                    [:meta [:name "meter"]] [:meter [:int "6"] [:int "8"]]]
                   [:beat
                    [:duration-dynamic "beat"]
                    [:atom
                     [:kind
                      [:name "note"]
                      [:arguments [:string "'C'"]]]]]]]
            want [:track
                   [:statement
                     ; [:header [:meta 'meter] [6 8]]
                     [:header
                      [:meta [:name "meter"]] [:meter [:int "6"] [:int "8"]]]
                     [:beat
                      (/ 1 8)
                      [:atom
                       [:kind
                        [:name "note"]
                        [:arguments [:string "'C'"]]]]]]]]
        (is (= want (track/resolve-durations tree)))))
    (testing "bar"
      (let [tree [:track
                  [:statement
                   [:header
                    [:meta [:name "meter"]] [:meter [:int "6"] [:int "8"]]]
                   [:beat
                    [:duration-dynamic "bar"]
                    [:atom
                     [:kind
                      [:name "note"]
                      [:arguments [:string "'C'"]]]]]]]
            want [:track
                   [:statement
                     [:header
                      [:meta [:name "meter"]] [:meter [:int "6"] [:int "8"]]]
                     [:beat
                      (/ 6 8)
                      [:atom
                       [:kind
                        [:name "note"]
                        [:arguments [:string "'C'"]]]]]]]]
        (is (= want (track/resolve-durations tree))))))
  (testing "static"
    (for [duration track/valid-divisors]
      (let [tree [:beat
                  [:duration-static (str duration "n")]
                  [:atom
                   [:kind
                    [:name "note"]
                    [:arguments [:string "'C'"]]]]]
              want [:beat
                    duration
                    [:atom
                     [:kind
                      [:name "note"]
                      [:arguments [:string "'C'"]]]]]]
          (is (= want (track/resolve-durations tree)))))))

(deftest valid-resolves?
  (testing "assign")
  (testing "beat")
  (testing "div")
  (testing "loop"))

(deftest pulse-beat
  (testing "provides the beat unit of the meter"
    (is (= (/ 1 8) (track/get-pulse-beat
                     [:header
                      [:meta [:name "meter"]] [:meter [:int "12"] [:int "8"]]])))))

(deftest step-beat
  (testing "provides the greatest common duration of beats in the track"
    (let [tree [:track
                [:beat
                 [:number "1"]
                 [:identifier :a]]
                [:beat
                 [:number "4"]
                 [:identifier :b]]
                [:beat
                 [:number "1/8"]
                 [:identifier :c]]]]
      (is (= (/ 1 8) (track/get-step-beat tree))))))

(deftest consume)
(deftest digest)
(deftest parse)
