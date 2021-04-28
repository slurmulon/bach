(ns bach.v3-integration-test
  (:require #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer-macros [deftest is testing run-tests]])
            ; https://github.com/clojure/test.check/blob/master/doc/intro.md#clojurescript
            [clojure.core.memoize :refer [memo memo-clear!]]
            [instaparse.core :as insta]
            [hiccup-find.core :refer [hiccup-find]]
            [bach.compose :as compose]
            [bach.ast :as ast]
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
    when 3 do { 1 -> :g }
  ]

  :part-b = 2 of [
    when 1 do {
      2 -> :a
      1 -> chord('Z')
    }
    1 -> :f
    1 -> :e
  ]

  ## Look a comment

  play! 2 of [:part-a :part-b]
  ")

(def fixture-bach-b
  "@Meter = 3|4
  @Tempo = 83

  :D = Chord('Dm')
  :G = Chord('G7')
  :C = Chord('Cmaj7')
  :F = Chord('Fmaj7')
  :B = Chord('Bm7b5')
  :E = Chord('E7')
  :A = Chord('A7')

  play! [
    4 of [
      3/4 -> {
        :D
        Scale('A aeolian')
      }
      3/4 -> :G
      3/4 -> :C
      3/4 -> :F
      3/4 -> :B
      3/4 -> :E
      3/4 -> :A
    ]

    3/4 -> :A
    3/4 -> :E
    3/4 -> :A
    3/4 -> :D
    3/4 -> :F
    3/4 -> :E
    2 * 3/4 -> :A
  ]")

(def fixture-bach-c
  "@Meter = 6|8
  @Tempo = 166

  :D = Chord('Dm')
  :G = Chord('G7')
  :C = Chord('Cmaj7')
  :F = Chord('Fmaj7')
  :B = Chord('Bm7b5')
  :E = Chord('E7')
  :A = Chord('A7')

  :part-a = [
    6/8 -> {
      :D
      Scale('A aeolian')
    }
    6/8 -> :G
    6/8 -> :C
    6/8 -> :F
    6/8 -> :B
    6/8 -> :E
  ]

  play! [
    3 of [
      :part-a
      6/8 -> Chord('Am')
      6/8 -> :A
    ]

    :part-a

    6/8 -> Chord('Am')
    6/8 -> :E
    6/8 -> :A
    6/8 -> :D
    6/8 -> :F
    6/8 -> :E
    2 * 6/8 -> Chord('Am')
  ]")

(def fixture-bach-d
  "@Tempo = 80

  play! [
    4 of [
      3/8 -> {
        Scale('E aeolian')
        Chord('Em')
      }
      5/8 -> Chord('Am')
      3/8 -> Chord('Bm')
      5/8 -> Chord('Cmaj7')
    ]

    2 of [
      1 -> Chord('Am')
      3/4 -> Chord('A')
      1/4 -> Chord('B')
      when 1 do { 2 -> Chord('E') }
      when 2 do { 2 -> Chord('B7b13') }
    ]
  ]")

(def fixture-bach-e
  ":a = stub('a')
  :b = stub('b')
  :c = stub('c')
  :e = stub('e')
  :f = stub('f')
  :g = stub('g')
  :h = stub('h')
  :i = stub('i')

  play! [
    8 of [
      2 -> :a
      bar -> :b
      when { 1 4 7 } do { 1 -> :c }
      when even? do { 1 -> :e }
      when 6..8 do { 1 -> :f }
      when !{ 1 last? } do { 1 -> :g }
      when [ even? !{2 4} ] do { 3 -> :h }
      when { gte? 5 lt? 3 } do { 1 -> :i }
    ]
  ]")

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


; (clojure.pprint/pprint (-> fixture-bach-b ast/parse compose/provision (select-keys [:headers :units])))
; (clojure.pprint/pprint (-> fixture-bach-b ast/parse track/get-step-beat))
; (clojure.pprint/pprint (-> fixture-bach-b ast/parse compose/provision-units))
; (clojure.pprint/pprint (-> fixture-bach-b compose/compose))
; (clojure.pprint/pprint (-> fixture-bach-b ast/parse track/digest))
(println "\n\nPlayable ======")
; (clojure.pprint/pprint (-> fixture-bach-b ast/parse track/playable))
; (clojure.pprint/pprint (-> fixture-bach-b ast/parse compose/provision))
; (clojure.pprint/pprint (-> fixture-bach-d ast/parse track/playable (compose/provision-signals {:unit 1/8 :meter 1})))
; (clojure.pprint/pprint (-> fixture-bach-d compose/compose time)) ;bach.data/to-json ));count))
; (clojure.pprint/pprint (-> fixture-bach-d compose/compose))
; (clojure.pprint/pprint (-> fixture-bach-e ast/parse track/parse compose/normalize-loops))
(clojure.pprint/pprint (-> fixture-bach-e compose/compose))
; (clojure.pprint/pprint (-> fixture-bach-e bach.ast/parse track/digest)) ;track/resolve-durations))

; (clojure.pprint/pprint (-> fixture-bach-d ast/parse time))
; (clojure.pprint/pprint (-> fixture-bach-d ast/parse track/playable track/get-durations))
; (clojure.pprint/pprint (-> fixture-bach-d ast/parse track/get-step-beat))
; (println (-> fixture-bach-d ast/parse compose/normalize-collections compose/as-durations))
; (clojure.pprint/pprint (-> fixture-bach-b ast/parse track/playable compose/unitize-collections))
; (clojure.pprint/pprint (->> fixture-bach-b ast/parse track/digest (hiccup-find [:header])))
; (println "norm duration" (-> fixture-bach-b track/resolve-values compose/provision-units (get-in [:beat :step])))
; (println "norm duration" (-> fixture-bach-b ast/parse compose/provision-headers))

