(ns bach.v3-compose-test
  (:require #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer-macros [deftest is testing run-tests]])
            ; #?(:clj [clojure.test.check.generators :as gen])
            ; https://github.com/clojure/test.check/blob/master/doc/intro.md#clojurescript
            [instaparse.core :as insta]
            [bach.compose :as compose]
            [bach.data :refer [to-ratio]]))

(def id-counter (atom 0))
(def reset-id! #(reset! id-counter 0))
(def next-id! #(swap! id-counter inc))
(def next-ids! #(take % (repeatedly next-id!)))

; Nested collections
;  - Ordered (lists) within unordered (sets)
;  - Simultaneous play signals, separate stop signals
(def fixture-a
  [:list
    [:pair
      [:number "1"]
      [:identifier :a]]
    [:set
      [:pair [:number "2"] [:identifier :b]]
      [:pair [:number "3"] [:identifier :c]]]
    [:set
      [:list
        [:pair [:number "4"] [:identifier :d]]
        [:pair [:number "5"] [:identifier :e]]]
      [:list
        [:pair [:number "6"] [:identifier :f]]
        [:set
          [:pair [:number "7"] [:identifier :g]]
          [:pair [:number "8"] [:identifier :h]]]]]])

; Nested collections
;  - Ordered (lists) within unordered (sets)
;  - Simultaneous play signals, simultaneous stop signals
(def fixture-b
  [:list
    [:pair
      [:number "1"]
      [:identifier :a]]
    [:set
      [:pair [:number "2"] [:identifier :b]]
      [:pair [:number "3"] [:identifier :c]]]
    [:set
      [:list
        [:pair [:number "4"] [:identifier :d]]
        [:pair [:number "5"] [:identifier :e]]]
      [:list
        [:pair [:number "4"] [:identifier :f]]
        [:pair [:number "6"] [:identifier :f]]]]])

(defn atomize-fixture
  [fixture]
  (insta/transform
    {:pair (fn [duration beat]
             [:pair
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
                  [:pair
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
    (testing "loops"
      (testing "root-level"
        (let [tree [:loop
                     [:number "2"]
                     [:list
                       [:pair
                         [:number "1"]
                         [:identifier :a]]
                       [:pair
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
      (testing "nested"
        (testing "in list"
          (let [tree [:list
                      [:pair
                       [:number "4"]
                       [:identifier :x]]
                      [:loop
                       [:number "2"]
                       [:list
                        [:pair
                         [:number "1"]
                         [:identifier :a]]
                          [:pair
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
              (is (= want (compose/normalize-collections tree))))))))
  (testing "durations"
    (testing "beats"
      (let [tree [:pair
                  [:number "3"]
                  [:identifier :a]]
            want 3]
      (is (= want (compose/normalize-durations tree)))))
    (testing "lists"
      (let [tree [:list
                  [:pair
                   [:number "1"]
                   [:identifier :a]]
                  [:pair
                   [:number "2"]
                   [:identifier :b]]
                  [:pair
                   [:number "3"]
                   [:identifier :c]]]
            want 6]
        (is (= want (compose/normalize-durations tree)))))
    (testing "sets"
      (let [tree [:set
                  [:pair
                   [:number "1"]
                   [:identifier :a]]
                  [:pair
                   [:number "4"]
                   [:identifier :b]]
                  [:pair
                   [:number "2"]
                   [:identifier :c]]]
            want 4]
        (is (= want (compose/normalize-durations tree))))))
  )
  ; (testing "beats"
  ;   (testing "position"
  ;     (let [tree [:list
  ;                 [:pair
  ;                  [:number "1"]
  ;                  [:identifier :a]]
  ;                 [:set
  ;                  [:pair [:number "2"] [:identifier :b]]
  ;                  [:pair [:number "3"] [:identifier :c]]]
  ;                 [:set
  ;                  [:pair [:number "4"] [:identifier :d]]
  ;                  [:list
  ;                   [:pair [:number "5"] [:identifier :e]]
  ;                   [:pair [:number "6"] [:identifier :f]]]]]
  ;           want false
  ;           ; actual (compose/position-beats tree)]
  ;           actual (compose/position-beats tree)]
  ;       (println "!!! position" actual)
  ;       ; (is (= want actual))))))
  ;       (is (= want false))))))

(deftest transpose-tree
  (testing "collections"
    ; (testing "list nested in sets"
    ; (let [tree [:list
    ;               [:pair
    ;                [:number "1"]
    ;                [:identifier :a]]
    ;               [:set
    ;                [:pair [:number "2"] [:identifier :b]]
    ;                [:pair [:number "3"] [:identifier :c]]]
    ;               [:set
    ;                [:pair [:number "4"] [:identifier :d]]
    ;                [:list
    ;                 [:pair [:number "5"] [:identifier :e]]
    ;                 [:pair [:number "6"] [:identifier :f]]]]]
    (let [tree [:list
                [:pair
                 [:number "1"]
                 [:identifier :a]]
                [:set
                 [:pair [:number "2"] [:identifier :b]]
                 [:pair [:number "3"] [:identifier :c]]]
                [:set
                 [:list
                  [:pair [:number "4"] [:identifier :d]]
                  [:pair [:number "5"] [:identifier :e]]]
                 [:list
                  [:pair [:number "6"] [:identifier :f]]
                  [:pair [:number "7"] [:identifier :g]]]]]
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
                  [:pair
                   [:number "1"]
                   [:identifier :a]]
                  [:set
                   [:pair [:number "2"] [:identifier :b]]
                   [:pair [:number "3"] [:identifier :c]]]
                  [:set
                   [:list
                    [:pair [:number "4"] [:identifier :d]]
                    [:pair [:number "5"] [:identifier :e]]]
                   [:list
                    [:pair [:number "6"] [:identifier :f]]
                    [:pair [:number "7"] [:identifier :g]]]]]
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
      (clojure.pprint/pprint actual)
      (is (= want actual)))))

(deftest signals
  (with-redefs [compose/element-id next-id!]
    (testing "play"
      (reset-id!)
      (let [tree (atomize-fixture fixture-a)
            actual (-> tree compose/normalize-beats compose/element-play-signals)
            want [["stub:1"]
                  ["stub:2" "stub:3"]
                  nil
                  nil
                  ["stub:4" "stub:6"]
                  nil
                  nil
                  nil
                  nil
                  nil
                  ["stub:5" "stub:7" "stub:8"]
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
       (reset-id!)
       (let [tree (atomize-fixture fixture-a)
             actual (-> tree compose/normalize-beats compose/element-stop-signals)
             want [["stub:8"]
                   ["stub:1"]
                   nil
                   ["stub:2"]
                   ["stub:3"]
                   nil
                   nil
                   nil
                   ["stub:4"]
                   nil
                   ["stub:6"]
                   nil
                   nil
                   nil
                   nil
                   ["stub:5"]
                   nil
                   ["stub:7"]]]
         (is (= want actual))))
     (testing "simultaneous occurence"
       (reset-id!)
       (let [tree (atomize-fixture fixture-b)
             actual (-> tree compose/normalize-beats compose/element-stop-signals)
             want [["stub:7"]
                   ["stub:1"]
                   nil
                   ["stub:2"]
                   ["stub:3"]
                   nil
                   nil
                   nil
                   ["stub:4" "stub:6"]
                   nil
                   nil
                   nil
                   nil
                   ["stub:5"]]]
         (is (= want actual)))))))

(deftest provision
  (with-redefs [compose/element-id next-id!]
    (testing "elements"
      (reset-id!)
      (let [tree (atomize-fixture fixture-a)
            actual (-> tree compose/normalize-beats compose/provision-elements)
            want {:stub
                   {"stub:1" {:value "a", :props ()},
                    "stub:2" {:value "b", :props ()},
                    "stub:3" {:value "c", :props ()},
                    "stub:6" {:value "f", :props ()},
                    "stub:4" {:value "d", :props ()},
                    "stub:8" {:value "h", :props ()},
                    "stub:5" {:value "e", :props ()},
                    "stub:7" {:value "g", :props ()}}}]
        (is (= want actual))))))

