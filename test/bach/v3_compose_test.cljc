(ns bach.v3-compose-test
  (:require #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer-macros [deftest is testing run-tests]])
            ; https://github.com/clojure/test.check/blob/master/doc/intro.md#clojurescript
            [clojure.core.memoize :refer [memo memo-clear!]]
            [instaparse.core :as insta]
            [bach.compose :as compose]
            [bach.data :refer [to-ratio]]))

; For more idiomatic solution
; @see: https://clojuredocs.org/clojure.spec.alpha/map-of#example-5cd31663e4b0ca44402ef71c
(def id-counter (atom 0))
(def next-id! #(swap! id-counter inc))
(def next-ids! #(take % (repeatedly next-id!)))
(def clear! #(do (reset! id-counter 0) (compose/clear!)))

; Nested collections
;  - Ordered (lists) within unordered (sets)
;  - Simultaneous play signals, separate stop signals
(def fixture-a
  [:list
    [:beat
      [:number "1"]
      [:identifier :a]]
    [:set
      [:beat [:number "2"] [:identifier :b]]
      [:beat [:number "3"] [:identifier :c]]]
    [:set
      [:list
        [:beat [:number "4"] [:identifier :d]]
        [:beat [:number "5"] [:identifier :e]]]
      [:list
        [:beat [:number "6"] [:identifier :f]]
        [:set
          [:beat [:number "7"] [:identifier :g]]
          [:beat [:number "8"] [:identifier :h]]]]]])

; Nested collections
;  - Ordered (lists) within unordered (sets)
;  - Simultaneous play signals, simultaneous stop signals
(def fixture-b
  [:list
    [:beat
      [:number "1"]
      [:identifier :a]]
    [:set
      [:beat [:number "2"] [:identifier :b]]
      [:beat [:number "3"] [:identifier :c]]]
    [:set
      [:list
        [:beat [:number "4"] [:identifier :d]]
        [:beat [:number "5"] [:identifier :e]]]
      [:list
        [:beat [:number "4"] [:identifier :f]]
        [:beat [:number "6"] [:identifier :g]]]]])

; Multiple identical elements
(def fixture-c
  [:list
   [:beat
    [:number "1"]
    [:identifier :a]]
   [:beat
    [:number "2"]
    [:identifier :a]]
   [:beat
    [:number "3"]
    [:identifier :b]]])

(defn atomize-fixture
  [fixture]
  (insta/transform
    {:beat (fn [duration beat]
             [:beat
               duration
               [:atom
                 [:keyword [:name "stub"]]
                 [:arguments [:string (->> beat last name (format "'%s'"))]]]])}
    fixture))

; @see https://cljdoc.org/d/leiningen/leiningen/2.9.5/api/leiningen.test
; (deftest ^:v3 normalization
(deftest normalize-tree
  (testing "collections"
    (testing "sets"
      (let [tree [:list
                  [:beat
                   [:number "1"]
                   [:identifier :a]]
                  [:set
                   [:identifier :b]
                   [:identifier :c]]
                  [:set
                   [:identifier :d]
                   [:list
                    [:identifier :e]
                    [:identifier :f]]]]
            want [{:duration 1,
                   :elements [:identifier :a]}
                  #{[:identifier :c] [:identifier :b]}
                  #{[[:identifier :e] [:identifier :f]] [:identifier :d]}]]
        (is (= want (compose/normalize-collections tree)))))
    (testing "lists" nil)))

; normalize-loops
(deftest loops
  (testing "basic"
    (let [tree [:loop
                  [:number "2"]
                  [:list
                    [:beat
                      [:number "1"]
                      [:identifier :a]]
                    [:beat
                      [:number "3"]
                      [:identifier :b]]]]
          want [{:duration 1
                  :elements [:identifier :a]}
                {:duration 3
                  :elements [:identifier :b]}
                {:duration 1
                  :elements [:identifier :a]}
                {:duration 3
                  :elements [:identifier :b]}]]
        (is (= want (compose/normalize-collections tree)))))
  (testing "containing"
    (testing "list"
      (let [tree [:list
                  [:beat
                   [:number "4"]
                   [:identifier :x]]
                  [:loop
                   [:number "2"]
                   [:list
                    [:beat
                      [:number "1"]
                      [:identifier :a]]
                    [:beat
                      [:number "3"]
                      [:identifier :b]]]]]
            want [{:duration 4
                   :elements [:identifier :x]}
                  [{:duration 1
                    :elements [:identifier :a]}
                   {:duration 3
                    :elements [:identifier :b]}
                   {:duration 1
                    :elements [:identifier :a]}
                   {:duration 3
                    :elements [:identifier :b]}]]]
          (is (= want (compose/normalize-collections tree)))))
    (testing "parallel lists"
      (let [tree [:set
                  [:beat
                   [:number "4"]
                   [:identifier :x]]
                  [:loop
                   [:number "2"]
                   [:list
                    [:beat
                      [:number "1"]
                      [:identifier :a]]
                    [:beat
                      [:number "3"]
                      [:identifier :b]]]]
                  [:loop
                   [:number "2"]
                   [:list
                    [:beat
                      [:number "2"]
                      [:identifier :c]]
                    [:beat
                      [:number "5"]
                      [:identifier :d]]]]]
            actual (compose/normalize-collections tree)
            want #{{:duration 4, :elements [:identifier :x]}
                   [{:duration 1, :elements [:identifier :a]}
                    {:duration 3, :elements [:identifier :b]}
                    {:duration 1, :elements [:identifier :a]}
                    {:duration 3, :elements [:identifier :b]}]
                   [{:duration 2, :elements [:identifier :c]}
                    {:duration 5, :elements [:identifier :d]}
                    {:duration 2, :elements [:identifier :c]}
                    {:duration 5, :elements [:identifier :d]}]}]
          (is (= want actual))))
    (testing "nested loop"
      (let [tree [:list
                  [:beat
                   [:number "1"]
                   [:identifier :a]]
                  [:loop
                   [:number "2"]
                   [:loop
                    [:number "2"]
                    [:list
                      [:beat
                        [:number "2"]
                        [:identifier :b]]
                      [:beat
                        [:number "3"]
                        [:identifier :c]]]]]]
            actual (compose/normalize-collections tree)
            want [{:duration 1, :elements [:identifier :a]}
                  [{:duration 2, :elements [:identifier :b]}
                    {:duration 3, :elements [:identifier :c]}
                    {:duration 2, :elements [:identifier :b]}
                    {:duration 3, :elements [:identifier :c]}
                    {:duration 2, :elements [:identifier :b]}
                    {:duration 3, :elements [:identifier :c]}
                    {:duration 2, :elements [:identifier :b]}
                    {:duration 3, :elements [:identifier :c]}]]]
          (is (= want actual))))
    ; (testing "in set"
    (testing "whens"
      (testing "basic"
        (let [tree [:loop
                    [:number "2"]
                      [:list
                      [:when
                        [:number "1"]
                        [:beat
                          [:number "1"]
                          [:identifier :a]]]
                      [:when
                        [:number "2"]
                        [:beat
                          [:number "1"]
                          [:identifier :b]]]]]
              actual (-> tree compose/reduce-values compose/normalize-loops)
              want [:list [:beat 1 [:identifier :a]]
                          [:beat 1 [:identifier :b]]]]
          (is (= want actual))))
      (testing "mixed"
        (let [tree [:loop
                    [:number "2"]
                    [:list
                     [:when
                      [:number "1"]
                      [:beat
                       [:number "1"]
                       [:identifier :a]]]
                     [:when
                       [:number "2"]
                       [:beat
                         [:number "1"]
                         [:identifier :b]]]
                     [:beat
                       [:number "3"]
                       [:identifier :c]]]]
              actual (-> tree compose/reduce-values compose/normalize-loops)
              want [:list [:beat 1 [:identifier :a]]
                          [:beat 3 [:identifier :c]]
                          [:beat 1 [:identifier :b]]
                          [:beat 3 [:identifier :c]]]]
          ; (clojure.pprint/pprint actual)
          (is (= want actual))))
      (testing "nested"
        (let [tree [:loop
                    [:number "3"]
                    [:list
                     [:beat
                      [:number "1"]
                      [:identifier :x]]
                     [:when
                      [:number "1"]
                      [:loop
                       ; TODO: Test loop that has whens that don't account for all loop iters
                       ; [:number "3"]
                       [:number "2"]
                       [:list
                        [:beat
                         [:number "1"]
                         [:identifier :a]]
                        [:when
                         [:number "2"]
                         [:beat
                          [:number "2"]
                          [:identifier :b]]]]]]
                     [:when
                      [:number "1"]
                      [:beat
                       [:number "4"]
                       [:identifier :g]]]
                     [:beat
                      [:number "3"]
                      [:identifier :h]]]]
              want [:list
                    [:beat 1 [:identifier :x]]
                    [:list
                     [:beat 1 [:identifier :a]]
                     [:beat 1 [:identifier :a]]
                     [:beat 2 [:identifier :b]]]
                    [:beat 4 [:identifier :g]]
                    [:beat 3 [:identifier :h]]
                    [:beat 1 [:identifier :x]]
                    [:beat 3 [:identifier :h]]
                    [:beat 1 [:identifier :x]]
                    [:beat 3 [:identifier :h]]]
              actual (-> tree compose/reduce-values compose/normalize-loops)]
          (is (= want actual))))
      (testing "distant"
        (let [tree [:loop
                    [:number "3"]
                    [:list
                     [:beat
                      [:number "1"]
                      [:identifier :a]]
                     [:when
                      [:number "2"]
                      [:beat
                       [:number "2"]
                       [:identifier :c]]]
                     [:list
                      [:beat
                       [:number "1"]
                       [:identifier :b]]
                      [:when
                       [:number "3"]
                       [:beat
                        [:number "3"]
                        [:identifier :z]]]]]]
              want [:list
                    [:beat 1 [:identifier :a]]
                    [:list
                     [:beat 1 [:identifier :b]]
                     nil]
                    [:beat 1 [:identifier :a]]
                    [:beat 2 [:identifier :c]]
                    [:list
                     [:beat 1 [:identifier :b]]
                     nil]
                    [:beat 1 [:identifier :a]]
                    [:list
                     [:beat 1 [:identifier :b]]
                     [:beat 3 [:identifier :z]]]]
              actual (-> tree compose/reduce-values compose/normalize-loops)]
          ; (clojure.pprint/pprint (compose/normalize-collections actual))
          ; (clojure.pprint/pprint actual)
          (is (= want actual))))
      )))

(testing "durations"
  (testing "beats"
    (let [tree [:beat
                [:number "3"]
                [:identifier :a]]
          want 3]
    (is (= want (compose/normalize-durations tree)))))
  (testing "lists"
    (let [tree [:list
                [:beat
                  [:number "1"]
                  [:identifier :a]]
                [:beat
                  [:number "2"]
                  [:identifier :b]]
                [:beat
                  [:number "3"]
                  [:identifier :c]]]
          want 6]
      (is (= want (compose/normalize-durations tree)))))
  (testing "sets"
    (let [tree [:set
                [:beat
                  [:number "1"]
                  [:identifier :a]]
                [:beat
                  [:number "4"]
                  [:identifier :b]]
                [:beat
                  [:number "2"]
                  [:identifier :c]]]
          want 4]
      (is (= want (compose/normalize-durations tree))))))


; (testing "beats"
;   (testing "position"
;     (let [tree [:list
;                 [:beat
;                  [:number "1"]
;                  [:identifier :a]]
;                 [:set
;                  [:beat [:number "2"] [:identifier :b]]
;                  [:beat [:number "3"] [:identifier :c]]]
;                 [:set
;                  [:beat [:number "4"] [:identifier :d]]
;                  [:list
;                   [:beat [:number "5"] [:identifier :e]]
;                   [:beat [:number "6"] [:identifier :f]]]]]
;           want false
;           ; actual (compose/position-beats tree)]
;           actual (compose/position-beats tree)]
;       ; (is (= want actual))))))
;       (is (= want false))))))

(deftest transpose-tree
  (testing "collections"
    ; (testing "list nested in sets"
    ; (let [tree [:list
    ;               [:beat
    ;                [:number "1"]
    ;                [:identifier :a]]
    ;               [:set
    ;                [:beat [:number "2"] [:identifier :b]]
    ;                [:beat [:number "3"] [:identifier :c]]]
    ;               [:set
    ;                [:beat [:number "4"] [:identifier :d]]
    ;                [:list
    ;                 [:beat [:number "5"] [:identifier :e]]
    ;                 [:beat [:number "6"] [:identifier :f]]]]]
    (let [tree [:list
                [:beat
                 [:number "1"]
                 [:identifier :a]]
                [:set
                 [:beat [:number "2"] [:identifier :b]]
                 [:beat [:number "3"] [:identifier :c]]]
                [:set
                 [:list
                  [:beat [:number "4"] [:identifier :d]]
                  [:beat [:number "5"] [:identifier :e]]]
                 [:list
                  [:beat [:number "6"] [:identifier :f]]
                  [:beat [:number "7"] [:identifier :g]]]]]
          want [{:duration 1 :elements [:identifier :a]}
                #{{:duration 2 :elements [:identifier :b]}
                  {:duration 3 :elements [:identifier :c]}}
                [#{{:duration 4 :elements [:identifier :d]}
                  {:duration 6 :elements [:identifier :f]}}
                 #{{:duration 5 :elements [:identifier :e]}
                   {:duration 7 :elements [:identifier :g]}}]]
          actual (compose/transpose-collections tree)]
      (is (= want actual)))))

(deftest linearize-tree
  (testing "collections"
    (testing "set -> list"
      (let [tree [:list
                  [:beat
                   [:number "1"]
                   [:identifier :a]]
                  [:set
                   [:beat [:number "2"] [:identifier :b]]
                   [:beat [:number "3"] [:identifier :c]]]
                  [:set
                   [:list
                    [:beat [:number "4"] [:identifier :d]]
                    [:beat [:number "5"] [:identifier :e]]]
                   [:list
                    [:beat [:number "6"] [:identifier :f]]
                    [:beat [:number "7"] [:identifier :g]]]]]
            want [{:duration 1 :elements [:identifier :a]}
                  #{{:duration 2 :elements [:identifier :b]}
                    {:duration 3 :elements [:identifier :c]}}
                  #{{:duration 4 :elements [:identifier :d]}
                    {:duration 6 :elements [:identifier :f]}}
                  #{{:duration 5 :elements [:identifier :e]}
                    {:duration 7 :elements [:identifier :g]}}]
            actual (compose/linearize-collections tree)]
        (is (= want actual))))
    (testing "set -> list -> set"
      (let [tree fixture-a
            want [{:duration 1 :elements [:identifier :a]}
                  #{{:duration 2 :elements [:identifier :b]}
                    {:duration 3 :elements [:identifier :c]}}
                  #{{:duration 4 :elements [:identifier :d]}
                    {:duration 6 :elements [:identifier :f]}}
                  #{{:duration 5 :elements [:identifier :e]}
                    {:duration 7 :elements [:identifier :g]}
                    {:duration 8 :elements [:identifier :h]}}]
            actual (compose/linearize-collections tree)]
        (is (= want actual))))))

(deftest beats
  (testing "linearize"
    (let [tree fixture-a
          want [{:items #{{:duration 1, :elements [:identifier :a]}},
                 :duration 1,
                 :index 0}
                {:items
                 #{{:duration 3, :elements [:identifier :c]}
                   {:duration 2, :elements [:identifier :b]}},
                 :duration 3,
                 :index 1}
                {:items
                 #{{:duration 4, :elements [:identifier :d]}
                   {:duration 6, :elements [:identifier :f]}},
                 :duration 6,
                 :index 4}
                {:items
                 #{{:duration 8, :elements [:identifier :h]}
                   {:duration 7, :elements [:identifier :g]}
                   {:duration 5, :elements [:identifier :e]}},
                 :duration 8,
                 :index 10}]
          actual (compose/linearize-beats tree)]
      (is (= want actual))))
  (testing "normalize"
    (let [tree fixture-a
          want [{:items #{{:duration 2, :elements [:identifier :a]}},
                 :duration 2,
                 :index 0}
                {:items
                 #{{:duration 6, :elements [:identifier :c]}
                   {:duration 4, :elements [:identifier :b]}},
                 :duration 6,
                 :index 2}
                {:items
                 #{{:duration 12, :elements [:identifier :f]}
                   {:duration 8, :elements [:identifier :d]}},
                 :duration 12,
                 :index 8}
                {:items
                 #{{:duration 16, :elements [:identifier :h]}
                   {:duration 10, :elements [:identifier :e]}
                   {:duration 14, :elements [:identifier :g]}},
                 :duration 16,
                 :index 20}]
          actual (compose/normalize-beats tree 1/2 1)]
      (is (= want actual)))))

(deftest signals
  (with-redefs [compose/uid next-id!]
    (testing "play"
      (clear!)
      (let [tree (atomize-fixture fixture-a)
            actual (-> tree compose/normalize-beats compose/element-play-signals)
            want [["stub.1"]
                  ["stub.2" "stub.3"]
                  nil
                  nil
                  ["stub.4" "stub.6"]
                  nil
                  nil
                  nil
                  nil
                  nil
                  ["stub.5" "stub.7" "stub.8"]
                  nil
                  nil
                  nil
                  nil
                  nil
                  nil
                  nil]]
        (is (= want actual))))
   (testing "stop"
     (testing "separate occurence"
       (clear!)
       (let [tree (atomize-fixture fixture-a)
             actual (-> tree compose/normalize-beats compose/element-stop-signals)
             want [["stub.8"]
                   ["stub.1"]
                   nil
                   ["stub.2"]
                   ["stub.3"]
                   nil
                   nil
                   nil
                   ["stub.4"]
                   nil
                   ["stub.6"]
                   nil
                   nil
                   nil
                   nil
                   ["stub.5"]
                   nil
                   ["stub.7"]]]
         (is (= want actual))))
     (testing "simultaneous occurence"
       (clear!)
       (let [tree (atomize-fixture fixture-b)
             actual (-> tree compose/normalize-beats compose/element-stop-signals)
             want [["stub.7"]
                   ["stub.1"]
                   nil
                   ["stub.2"]
                   ["stub.3"]
                   nil
                   nil
                   nil
                   ["stub.4" "stub.6"]
                   nil
                   nil
                   nil
                   nil
                   ["stub.5"]]]
         ; (clojure.pprint/pprint actual)
         (is (= want actual)))))
   (testing "pulse beats"
     (clear!)
     (let [tree (atomize-fixture fixture-a)
           actual (compose/pulse-beat-signals tree)
           want [0 1 1 1 2 2 2 2 2 2 3 3 3 3 3 3 3 3]]
       (is (= want actual))))))

(deftest provision
  (with-redefs [compose/uid next-id!]
    (testing "elements"
      (testing "all unique"
        (clear!)
        (let [tree (atomize-fixture fixture-a)
              actual (-> tree compose/normalize-beats compose/provision-elements)
              want {:stub
                    {"1" {:value "a", :props ()},
                     "2" {:value "b", :props ()},
                     "3" {:value "c", :props ()},
                     "6" {:value "f", :props ()},
                     "4" {:value "d", :props ()},
                     "8" {:value "h", :props ()},
                     "5" {:value "e", :props ()},
                     "7" {:value "g", :props ()}}}]
          (is (= want actual))))
      (testing "some identical"
        (clear!)
        (let [tree (atomize-fixture fixture-c)
              actual (-> tree compose/normalize-beats compose/provision-elements)
              want {:stub
                    {"1" {:value "a", :props ()},
                     "2" {:value "b", :props ()}}}]
          (is (= want actual)))))
   (testing "beats"
      (clear!)
      (let [tree (atomize-fixture fixture-a)
            actual (-> tree compose/normalize-beats compose/provision-beats)
            want [{:items
                    [{:duration 1, :elements ["stub.1"]}],
                   :duration 1,
                   :index 0}
                  {:items
                    [{:duration 2, :elements ["stub.2"]}
                     {:duration 3, :elements ["stub.3"]}],
                   :duration 3,
                   :index 1}
                  {:items
                    [{:duration 4, :elements ["stub.4"]}
                     {:duration 6, :elements ["stub.6"]}],
                   :duration 6,
                   :index 4}
                  {:items
                    [{:duration 5, :elements ["stub.5"]}
                     {:duration 7, :elements ["stub.7"]}
                     {:duration 8, :elements ["stub.8"]}],
                   :duration 8,
                   :index 10}]]
        ; (clojure.pprint/pprint actual)
        (is (= want actual))))))

; (clojure.pprint/pprint (->> fixture-a atomize-fixture (conj [:play]) compose/get-play))
; (clojure.pprint/pprint (-> fixture-a atomize-fixture compose/playable))
; (clojure.pprint/pprint (-> fixture-a atomize-fixture compose/compose))
; (clojure.pprint/pprint (-> fixture-a atomize-fixture compose/parse))
; (clojure.pprint/pprint (-> fixture-a atomize-fixture compose/provision))
; (clojure.pprint/pprint (-> [:loop [:number "2"] [:list [:string "'a'"] [:string "'z'"]]] compose/reduce-values compose/normalize-loops))

; (println "@@@@@@@@@@@")
; (clojure.pprint/pprint (-> [:list [:play [:a 1]] [:play [:b 2]]] (compose/get-play)))

(def fixture-bach-a
  "
  :a = chord('A7')
  :e = chord('E6')
  :g = chord('Gm')
  :f = chord('F#')

  :part-a = 3 of [
    3 -> { :a, scale('A dorian') }
    2 -> :e
    when 3 then { 1 -> :g }
  ]

  :part-b = 2 of [
    when 1 then { 2 -> :a, 1 -> chord('Z') }
    1 -> :f
    1 -> :e
  ]

  play! 1 of [:part-a :part-b]
  ")

(println "@@@@@@")
; (clojure.pprint/pprint (compose/compose fixture-bach-a))
; (clojure.pprint/pprint (-> fixture-bach-a compose/get-play compose/reduce-track))
; (clojure.pprint/pprint (-> fixture-bach-a bach.ast/parse))
; (clojure.pprint/pprint (-> fixture-bach-a bach.ast/parse compose/reduce-values))
; (clojure.pprint/pprint (-> fixture-bach-a bach.ast/parse compose/digest))
; (clojure.pprint/pprint (-> fixture-bach-a bach.ast/parse compose/provision))
; (clojure.pprint/pprint (-> fixture-bach-a
;                            bach.ast/parse
;                            compose/reduce-track
;                            compose/get-play
;                            compose/normalize-beats))
                           ; compose/normalize-collections)); compose/itemize-beats))
(clojure.pprint/pprint (-> fixture-bach-a compose/compose))
(clojure.pprint/pprint (-> fixture-bach-a bach.ast/parse compose/digest compose/validate-2?))

; (clojure.pprint/pprint (bach.ast/parse fixture-bach-a))
; (clojure.pprint/pprint (bach.ast/parse "[1 -> :a, 2 -> :b]"))
; (clojure.pprint/pprint (bach.ast/parse "[when 1, when 2]"))
; (clojure.pprint/pprint (bach.ast/parse "[when 1 then [ 1 -> :a ], when 2 then { 2 -> :b }]"))
; (clojure.pprint/pprint (bach.ast/parse "[when 1 then [ 1 -> :a ]]"))
; (clojure.pprint/pprint (bach.ast/parse "[when 1 then :a]"))
