(ns ^:eftest/synchronized bach.track-test
  (:require #?@(:clj [[clojure.test :refer [deftest is testing]]]
               :cljs [[bach.crypto]
                      [cljs.test :refer-macros [deftest is testing run-tests]]])
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
                    [:meta [:name "meter"]]
                    [:meter [:int "6"] [:int "8"]]]
                   [:beat
                    [:duration-dynamic "bar"]
                    [:set]]]]
            want [:track
                   [:statement
                     [:header
                      [:meta [:name "meter"]]
                      [:meter [:int "6"] [:int "8"]]]
                     [:beat
                      (/ 6 8)
                      [:set]]]]]
        (is (= want (track/resolve-durations tree))))))
  (testing "static"
    (doseq [duration track/valid-divisors]
      (let [tree [:beat
                  [:duration-static (str duration)]
                  [:set]]
              want [:beat (/ 1 duration) [:set]]]
          (is (= want (track/resolve-durations tree)))))))

(deftest valid-resolves?
  (testing "assign")
  (testing "beat"
    (testing "duration"
      (testing "must be between 0 and max valid duration"
        (doseq [duration (list 0 8 79 track/valid-max-duration)]
          (is (= true (track/valid-resolves? [:beat duration [:set]]))))
        ; FIXME: -1 should be in this spec but breaks integration test when logic is updated
        (doseq [duration (list (inc track/valid-max-duration))]
          (let [tree [:beat duration [:set]]]
            (is (thrown-with-msg? #?(:clj Exception :cljs js/Error) #"Beat durations" (track/valid-resolves? tree)))))
        ))
    (testing "value"
      (testing "must be an atom, rest or set"
        (doseq [value (list :atom :rest :set)]
          (is (= true (track/valid-resolves? [:beat 1 [value]]))))
        (doseq [value (list :list :play :boom)]
          (let [tree [:beat 1 [value]]]
            (is (thrown-with-msg? #?(:clj Exception :cljs js/Error) #"Beat values" (track/valid-resolves? tree))))))))
  (testing "div")
  (testing "loop"
    (testing "value must be a list or set"
        (doseq [value (list :set :list)]
          (is (= true (track/valid-resolves? [:loop 3 [value]]))))
        (doseq [value (list :rest :play :boom)]
          (let [tree [:loop 3 [value]]]
            (is (thrown-with-msg? #?(:clj Exception :cljs js/Error) #"Loop values" (track/valid-resolves? tree))))))))

(deftest valid-tempo?
  (testing "returns true when between 0 and max tempo"
    (is (= true (track/valid-tempo? [:header [:meta [:name "tempo"]] [:number "1"]])))
    (is (= true (track/valid-tempo? [:header [:meta [:name "tempo"]] [:number "120"]])))
    (is (= true (track/valid-tempo? [:header [:meta [:name "tempo"]] [:number (str track/valid-max-tempo)]]))))
  (testing "throws problem when outside of 0 and max tempo"
    (doseq [tempo (list 0 -1 (inc track/valid-max-tempo))]
      (let [tree [:header [:meta [:name "tempo"]] [:number (str tempo)]]]
        (is (thrown-with-msg? #?(:clj Exception :cljs js/Error) #"Tempos must be between" (track/valid-tempo? tree)))))))

(deftest valid-meter?
  (testing "returns true when pulse beat divisor is valid"
    (doseq [divisor (rest track/valid-divisors)]
      (is (= true (track/valid-meter?
                    [:header
                     [:meta [:name "meter"]]
                     [:meter [:number "1"] [:number (str divisor)]]]))))
  (testing "throws problem when pulse beat divisor is not even or greater than max valid divisor"
    (doseq [divisor (list 5 9 72 257 (inc track/valid-max-duration))]
      (let [tree [:header
                  [:meta [:name "meter"]]
                  [:meter [:number "1"] [:number (str divisor)]]]]
        (is (thrown-with-msg? #?(:clj Exception :cljs js/Error) #"Meter unit beats" (track/valid-meter? tree))))))))

(deftest valid-play?
  (testing "returns true when track has a single play! export"
    (is (= true (track/valid-play? [:play :a]))))
  (testing "throws problem when track has no play! export")
    (is (thrown-with-msg? #?(:clj Exception :cljs js/Error) #"Exactly one Play" (track/valid-play? [:list])))
  (testing "throws problem when track has multiple play! exports")
    (let [tree [:statement [:play :a] [:play :b]]]
      (is (thrown-with-msg? #?(:clj Exception :cljs js/Error) #"Exactly one Play" (track/valid-play? tree)))))

(deftest get-pulse-beat
  (testing "provides the beat unit of the meter"
    (is (= (/ 1 8) (track/get-pulse-beat
                     [:header
                      [:meta [:name "meter"]] [:meter [:int "12"] [:int "8"]]])))))

(deftest get-step-beat
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

(deftest get-pulse-beat-time
  (testing "determines number of milliseconds equal to one pulse-beat duration"
    (doseq [[want tempo] [[500.0 120] [750.0 80] [1000.0 60]]]
      (let [tree [:header [:meta [:name "tempo"]] [:number (str tempo)]]]
        (is (= want (track/get-pulse-beat-time tree)))))))

(deftest get-step-beat-time
  (testing "determines number of milliseconds equal to one step-beat duration"
    (doseq [[want tempo meter duration] [[250.0 120 [4 4] (/ 1 8)]
                                         [250.0 120 [5 8] (/ 1 16)]
                                         [500.0 120 [6 8] (/ 1 8)]
                                         [1000.0 120 [12 8] (/ 1 4)]
                                         [1000.0 120 [2 2] 4]]]
      (let [tree [:track
                  [:header
                   [:meta [:name "tempo"]]
                   [:number (str tempo)]]
                  [:header
                   [:meta [:name "meter"]]
                   [:meter [:int (str (first meter))] [:int (str (last meter))]]]
                  [:beat duration [:set]]]]
        (is (= want (track/get-step-beat-time tree)))))))

(deftest consume)
(deftest digest)
(deftest parse)
