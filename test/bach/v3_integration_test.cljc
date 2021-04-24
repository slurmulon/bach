(ns bach.v3-integration-test
  (:require #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer-macros [deftest is testing run-tests]])
            ; https://github.com/clojure/test.check/blob/master/doc/intro.md#clojurescript
            [clojure.core.memoize :refer [memo memo-clear!]]
            [instaparse.core :as insta]
            [bach.compose :as compose]
            [bach.track :as track]))

(def id-counter (atom 0))
(def next-id! #(swap! id-counter inc))
(def next-ids! #(take % (repeatedly next-id!)))
(def clear! #(do (reset! id-counter 0) (compose/clear!)))

(def fixture-bach-a
  "@Tempo = 150

  :a = chord('A7')
  :e = chord('E6')
  :g = chord('Gm')
  :f = chord('F#')

  :part-a = 3 of [
    3 -> { :a, scale('A dorian') }
    2 -> :e
    when 3 { 1 -> :g }
  ]

  :part-b = 2 of [
    when 1 {
      2 -> :a
      1 -> chord('Z')
    }
    1 -> :f
    1 -> :e
  ]

  ## Look a comment

  play! 2 of [:part-a :part-b]
  ")

(deftest compose
  (with-redefs [compose/uid next-id!]
    (testing "basic"
      (clear!)
      (let [actual (compose/compose fixture-bach-a)
            want {:iterations 2,
                  :headers {:tempo 150, :meter [4 4]},
                  :units
                  {:beat {:step 1, :pulse 1/4},
                   :bar {:step 1, :pulse 4},
                   :time {:step 1600.0, :pulse 400.0, :bar 1600.0}},
                  :metrics {:min 1, :max 3, :total 22},
                  :elements
                  {:scale {"2" {:value "A dorian", :props []}},
                   :chord
                    {"1" {:value "A7", :props []},
                     "3" {:value "E6", :props []},
                     "4" {:value "Gm", :props []},
                     "5" {:value "Z", :props []},
                     "6" {:value "F#", :props []}}},
                  :signals
                  {:beat [0 0 0 1 1 2 2 2 3 3 4 4 4 5 5 6 7 7 8 9 10 11],
                   :play
                    [["scale.2" "chord.1"]
                     nil
                     nil
                     ["chord.3"]
                     nil
                     ["scale.2" "chord.1"]
                     nil
                     nil
                     ["chord.3"]
                     nil
                     ["scale.2" "chord.1"]
                     nil
                     nil
                     ["chord.3"]
                     nil
                     ["chord.4"]
                     ["chord.5" "chord.1"]
                     nil
                     ["chord.6"]
                     ["chord.3"]
                     ["chord.6"]
                     ["chord.3"]],
                    :stop
                    [["chord.3"]
                      nil
                      nil
                      ["scale.2" "chord.1"]
                      nil
                      ["chord.3"]
                      nil
                      nil
                      ["scale.2" "chord.1"]
                      nil
                      ["chord.3"]
                      nil
                      nil
                      ["scale.2" "chord.1"]
                      nil
                      ["chord.3"]
                      ["chord.4"]
                      ["chord.5"]
                      ["chord.1"]
                      ["chord.6"]
                      ["chord.3"]
                      ["chord.6"]]},
                  :beats
                  [{:items [{:duration 3, :elements ["scale.2" "chord.1"]}],
                    :duration 3,
                    :index 0}
                   {:items [{:duration 2, :elements ["chord.3"]}],
                    :duration 2,
                    :index 3}
                   {:items [{:duration 3, :elements ["scale.2" "chord.1"]}],
                    :duration 3,
                    :index 5}
                   {:items [{:duration 2, :elements ["chord.3"]}],
                    :duration 2,
                    :index 8}
                   {:items [{:duration 3, :elements ["scale.2" "chord.1"]}],
                    :duration 3,
                    :index 10}
                   {:items [{:duration 2, :elements ["chord.3"]}],
                    :duration 2,
                    :index 13}
                   {:items [{:duration 1, :elements ["chord.4"]}],
                    :duration 1,
                    :index 15}
                   {:items
                    [{:duration 1, :elements ["chord.5"]}
                     {:duration 2, :elements ["chord.1"]}],
                    :duration 2,
                    :index 16}
                   {:items [{:duration 1, :elements ["chord.6"]}],
                    :duration 1,
                    :index 18}
                   {:items [{:duration 1, :elements ["chord.3"]}],
                    :duration 1,
                    :index 19}
                   {:items [{:duration 1, :elements ["chord.6"]}],
                    :duration 1,
                    :index 20}
                   {:items [{:duration 1, :elements ["chord.3"]}],
                    :duration 1,
                    :index 21}]}]
        (clojure.pprint/pprint actual)
        (is (= want actual))))))

