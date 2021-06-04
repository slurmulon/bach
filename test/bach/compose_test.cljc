(ns ^:eftest/synchronized bach.compose-test
  (:require #?@(:clj [[clojure.test :refer [deftest is testing]]]
               :cljs [[bach.crypto]
                      [cljs.test :refer-macros [deftest is testing run-tests]]
                      [goog.string :as gstring]
                      ; [goog.string.format :as format]])
                      [goog.string.format]])
            [instaparse.core :as insta]
            [bach.compose :as compose]
            [bach.track :as track]
            [bach.data :as data]))

; For more idiomatic solution
; @see: https://clojuredocs.org/clojure.spec.alpha/map-of#example-5cd31663e4b0ca44402ef71c
(def id-counter (atom 0))
(def next-id! (fn [_] (swap! id-counter inc)))
(def clear! #(reset! id-counter 0))
(def norm #?(:clj identity :cljs clj->js))

; Nested collections
;  - Ordered (lists) within unordered (sets)
;  - Simultaneous play signals, separate stop signals
;  - Different durations between concurrent beats (i.e. in set) on same list index
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
        ; TODO: Make version of fixture using 4 here instead (same duration vs. diff duration)
        [:beat [:number "6"] [:identifier :f]]
        [:set
          [:beat [:number "7"] [:identifier :g]]
          [:beat [:number "8"] [:identifier :h]]]]]])

; Nested collections
;  - Ordered (lists) within unordered (sets)
;  - Simultaneous play signals, separate stop signals
;  - Same durations between concurrent beats (i.e. in set) on same list index (4, :d :f)
(def fixture-a2
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

; list in set with mis-aligned indices/durations
(def fixture-d
  [:set
   [:list
    [:beat
      [:number "2"]
      [:identifier :a1]]
    [:beat
      [:number "3"]
      [:identifier :a2]]]
   [:list
    [:beat
     [:number "1"]
     [:identifier :b1]]
    [:beat
     [:number "1"]
     [:identifier :b2]]
    [:beat
     [:number "1"]
     [:identifier :b3]]
    [:beat
     [:number "2"]
     [:identifier :b4]]]])

(def fixture-e
  [:list
   [:set
    [:list
      [:beat
        [:number "2"]
        [:identifier :a1]]
      [:beat
        [:number "3"]
        [:identifier :a2]]]
    [:list
      [:beat
        [:number "3"]
        [:identifier :b1]]
      [:beat
        [:number "2"]
        [:identifier :b2]]];]
    [:list
     [:beat
      [:number "1"]
      [:identifier :c1]]
     [:beat
      [:number "4"]
      [:identifier :c2]]]]])

; TODO!!!
;  - Need this for fixing (compose/normalize-beats (atomize-fixture fixture-bach-a))
; list -> set -> beat
(def fixture-f
  [:list
   [:set
    [:beat
     [:number "2"]
     [:identifier :a1]]
    [:beat
     [:number "3"]
     [:identifier :a2]]]
   [:beat
    [:number "4"]
    [:identifier :b1]]])

; contains gaps between notes that mostly overlap
; def fixture-g

(def fixture-h
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
        [:beat [:number "7"] [:identifier :g]]]]])

(defn atomize-fixture
  [fixture]
  (insta/transform
    {:beat (fn [duration beat]
             [:beat
               duration
               [:atom
                 [:kind [:name "stub"]]
                 [:arguments [:string (->> beat
                                           last
                                           name
                                           (#?(:clj format :cljs gstring/format) "'%s'"))]]]])}
    fixture))

; @see https://cljdoc.org/d/leiningen/leiningen/2.9.5/api/leiningen.test
; (deftest ^:v3 normalization
(deftest ^:eftest/synchronized normalize-tree
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
(deftest ^:eftest/synchronized loops
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
              actual (-> tree track/reduce-values compose/normalize-loops)
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
              actual (-> tree track/reduce-values compose/normalize-loops)
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
              actual (-> tree track/reduce-values compose/normalize-loops)]
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
              actual (-> tree track/reduce-values compose/normalize-loops)]
          (is (= want actual))))
      )))

(deftest ^:eftest/synchronized durations
  (testing "reduction"
    (testing "set -> lists"
      (let [tree #{[2 2] [2 1 1]}
            want 4
            actual (-> tree compose/reduce-durations)]
        (is (= want actual))))
    (testing "list -> sets"
      (let [tree [#{1 2} #{3 4} 5]
            want 9
            actual (-> tree compose/reduce-durations)]
        (is (= want actual))))))

(deftest ^:eftest/synchronized transpose-list
  (with-redefs [compose/uid (memoize next-id!)]
    (testing "list -> set -> beats"
      (clear!)
      (let [tree (-> fixture-f
                    atomize-fixture
                    compose/normalize-collections
                    (compose/unitize-durations 1))
            actual (compose/transpose-lists tree)
            want [[#{{:duration 2,
                      :elements [{:id "stub.1", :kind :stub, :props [], :value "a1"}]}
                    {:duration 3,
                      :elements [{:id "stub.2", :kind :stub, :props [], :value "a2"}]}}
                      nil]
                    {:duration 4,
                      :elements [{:id "stub.3", :kind :stub, :props [], :value "b1"}]}
                    nil
                    nil
                    nil]]
        (is (= want actual))))))
  ; (with-redefs [compose/uid (memoize next-id!)]
  ;   (testing "list -> set -> list -> set"
  ;     (clear!)
  ;     (let [tree (-> fixture-a
  ;                   atomize-fixture
  ;                   compose/normalize-collections
  ;                   (compose/unitize-durations 1))
  ;           actual (compose/transpose-lists tree)
  ;           want []]
  ;       (is (= want actual))))))

(deftest ^:eftest/synchronized transpose-set
  (with-redefs [compose/uid (memoize next-id!)]
    (testing "list -> set -> list"
      (clear!)
      (let [tree (-> fixture-h
                    atomize-fixture
                    compose/normalize-collections
                    (compose/unitize-durations 1))
            actual (compose/transpose-sets tree)
            want [{:duration 1,
                   :elements [{:id "stub.1", :kind :stub, :value "a", :props '()}]}
                  #{{:duration 2,
                     :elements [{:id "stub.2", :kind :stub, :value "b", :props '()}]}
                    {:duration 3,
                      :elements [{:id "stub.3", :kind :stub, :value "c", :props '()}]}}
                  [#{{:duration 4,
                      :elements [{:id "stub.4", :kind :stub, :value "d", :props '()}]}
                     {:duration 6,
                      :elements [{:id "stub.6", :kind :stub, :value "f", :props '()}]}}
                   #{{:duration 5,
                      :elements [{:id "stub.5", :kind :stub, :value "e", :props '()}]}
                     {:duration 7,
                      :elements [{:id "stub.7", :kind :stub, :value "g", :props '()}]}}]]]
        (is (= want actual))))))

(deftest ^:eftest/synchronized quantize-collections
  (testing "set -> list"
    (let [tree fixture-d
          want [#{{:duration 2, :elements '(:identifier :b1)}
                  {:duration 4, :elements '(:identifier :a1)}}
                nil
                #{{:duration 2, :elements '(:identifier :b2)}}
                nil
                #{{:duration 6, :elements '(:identifier :a2)}
                  {:duration 2, :elements '(:identifier :b3)}}
                nil
                #{{:duration 4, :elements '(:identifier :b4)}}
                nil
                nil
                nil]
          actual (compose/quantize-collections tree (/ 1 2))]
      ; (clojure.pprint/pprint actual)
      (is (= want actual))))
  (testing "list -> set -> list"
    (let [tree fixture-e
          want [#{{:duration 6, :elements [:identifier :b1]}
                  {:duration 4, :elements [:identifier :a1]}
                  {:duration 2, :elements [:identifier :c1]}}
                nil
                #{{:duration 8, :elements [:identifier :c2]}}
                nil
                #{{:duration 6, :elements [:identifier :a2]}}
                nil
                #{{:duration 4, :elements [:identifier :b2]}}
                nil
                nil
                nil]
          actual (compose/quantize-collections tree (/ 1 2))]
      ; (clojure.pprint/pprint actual)
      ; (clojure.pprint/pprint (-> actual bach.tree/squash))
      (is (= want actual))))
  (testing "list -> set -> list -> set (mis-aligned durations)"
    (let [tree fixture-a
          want [{:duration 1, :elements [:identifier :a]}
                #{{:duration 2, :elements [:identifier :b]}
                  {:duration 3, :elements [:identifier :c]}}
                nil
                #{{:duration 4, :elements [:identifier :d]}
                  {:duration 6, :elements [:identifier :f]}}
                nil
                nil
                nil
                #{{:duration 5, :elements [:identifier :e]}}
                nil
                #{{:duration 7, :elements [:identifier :g]}
                  {:duration 8, :elements [:identifier :h]}}
                nil
                nil
                nil
                nil
                nil
                nil
                nil
                nil]
          actual (compose/quantize-collections tree 1)]
      (is (= want actual))))
  (testing "list -> set -> list -> set (aligned durations)"
    (let [tree fixture-a2
          want [{:duration 1, :elements [:identifier :a]}
                #{{:duration 2, :elements [:identifier :b]}
                  {:duration 3, :elements [:identifier :c]}}
                nil
                #{{:duration 4, :elements [:identifier :d]}
                  {:duration 4, :elements [:identifier :f]}}
                nil
                nil
                nil
                #{{:duration 5, :elements [:identifier :e]}
                  {:duration 7, :elements [:identifier :g]}
                  {:duration 8, :elements [:identifier :h]}}
                nil
                nil
                nil
                nil
                nil
                nil
                nil
                nil
                nil
                nil]
          ; actual (compose/quantize-collections tree (/ 1 2))]
          actual (compose/quantize-collections tree 1)]
      (is (= want actual)))))

(deftest ^:eftest/synchronized beats
  (testing "provision"
    (with-redefs [compose/uid (memoize next-id!)]
      (clear!)
      (let [beats (-> fixture-a atomize-fixture (compose/normalize-beats (/ 1 2)))
            want [{:duration 2,
                   :id 0,
                   :index 0,
                   :items [{:duration 2, :elements ["stub.1"]}]}
                  {:duration 4,
                   :id 1,
                   :index 2,
                   :items [{:duration 4, :elements ["stub.2"]}
                           {:duration 6, :elements ["stub.3"]}]}
                  {:duration 8,
                   :id 2,
                   :index 6,
                   :items [{:duration 8, :elements ["stub.4"]}
                           {:duration 12, :elements ["stub.6"]}]}
                  {:duration 10,
                   :id 3,
                   :index 14,
                   :items [{:duration 10, :elements ["stub.5"]}]}
                  {:duration 14,
                   :id 4,
                   :index 18,
                   :items [{:duration 14, :elements ["stub.7"]}
                           {:duration 16, :elements ["stub.8"]}]}]
            actual (bach.tree/cast-tree sequential? vec (compose/provision-beats beats))]
        (is (= want actual))))))

(deftest ^:eftest/synchronized steps
  (testing "provisioned elements"
    (with-redefs [compose/uid (memoize next-id!)]
     (clear!)
     (let [tree (atomize-fixture fixture-e)
           actual (bach.tree/cast-tree sequential? vec (compose/provision-element-steps tree (/ 1 2)))
           want [["stub.1" "stub.3" "stub.5"]
                 ["stub.1" "stub.3" "stub.5"]
                 ["stub.1" "stub.3" "stub.6"]
                 ["stub.1" "stub.3" "stub.6"]
                 ["stub.2" "stub.3" "stub.6"]
                 ["stub.2" "stub.3" "stub.6"]
                 ["stub.2" "stub.4" "stub.6"]
                 ["stub.2" "stub.4" "stub.6"]
                 ["stub.2" "stub.4" "stub.6"]
                 ["stub.2" "stub.4" "stub.6"]]]
       (is (= want actual)))))
    (testing "provisioned beats"
     (with-redefs [compose/uid (memoize next-id!)]
      (clear!)
      (let [tree (atomize-fixture fixture-e)
            actual (bach.tree/cast-tree sequential? vec (compose/provision-beat-steps tree (/ 1 2)))
            want [0 0 1 1 2 2 3 3 3 3]]
        (is (= want actual)))))
    (testing "provisioned states"
    (with-redefs [compose/uid (memoize next-id!)]
      (clear!)
      (let [tree (atomize-fixture fixture-e)
            actual (bach.tree/cast-tree sequential? vec (compose/provision-state-steps tree (/ 1 2)))
            want [[0 "stub.1" "stub.3" "stub.5"]
                  [0 "stub.1" "stub.3" "stub.5"]
                  [1 "stub.1" "stub.3" "stub.6"]
                  [1 "stub.1" "stub.3" "stub.6"]
                  [2 "stub.2" "stub.3" "stub.6"]
                  [2 "stub.2" "stub.3" "stub.6"]
                  [3 "stub.2" "stub.4" "stub.6"]
                  [3 "stub.2" "stub.4" "stub.6"]
                  [3 "stub.2" "stub.4" "stub.6"]
                  [3 "stub.2" "stub.4" "stub.6"]]]
        (is (= want actual)))))
    (testing "provisioned plays"
    (with-redefs [compose/uid (memoize next-id!)]
     (clear!)
     (let [tree (atomize-fixture fixture-e)
           actual (bach.tree/cast-tree sequential? vec (-> tree (compose/normalize-beats (/ 1 2)) (compose/provision-play-steps)))
           want [["stub.1" "stub.3" "stub.5"]
                 []
                 ["stub.6"]
                 []
                 ["stub.2"]
                 []
                 ["stub.4"]
                 []
                 []
                 []]]
       (is (= want actual)))))
    (testing "provisioned stops"
     (with-redefs [compose/uid (memoize next-id!)]
     (clear!)
     (let [tree (atomize-fixture fixture-e)
           actual (-> tree (compose/normalize-beats (/ 1 2)) (compose/provision-stop-steps))
           want [["stub.2" "stub.4" "stub.6"]
                 []
                 ["stub.5"]
                 []
                 ["stub.1"]
                 []
                 ["stub.3"]
                 []
                 []
                 []]]
       (is (= want actual)))))
    ; INPROG
    ; (testing "provisioned unified steps"
    ;   (clear!)
    ;   (let [tree (atomize-fixture fixture-e)
    ;         actual (-> tree (compose/provision-steps 1/2))
    ;         want []]
    ;     (is (= want actual))
    ;         ))))
    );)
