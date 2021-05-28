(ns bach.v3-compose-test
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
(def found-elems (atom {}))
; (def next-id! bach.data/nano-hash)
; (def next-id! (memoize #(swap! id-counter inc)))
; (def next-id! #(swap! id-counter inc))
(def next-id! (fn [_] (swap! id-counter inc)))
; (def next-id! (memoize (fn [_] (swap! id-counter inc))))
(def next-ids! #(take % (repeatedly next-id!)))
(def clear! #(do (reset! id-counter 0) (reset! found-elems {})))

(def norm #?(:clj identity :cljs clj->js))

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
;  - Need this for fixing (compose/normalize-beats-2 (atomize-fixture fixture-bach-a))
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

(defn atomize-fixture
  [fixture]
  (insta/transform
    {:beat (fn [duration beat]
             [:beat
               duration
               [:atom
                 [:kind [:name "stub"]]
                 ; WARN: Fixes all cljc tests, borks cljs
                 ; [:arguments [:string (->> beat last name (#?(:clj format :cljs gstring/format) "'%s'"))]]]])}
                 [:arguments [:string (->> beat
                                           last
                                           name
                                           ; #?(:clj (format "'%s'")
                                           ;    ; WEIRD: Fixes most cljs tests, causes explosion on two
                                           ;    ; :cljs #(gstring/format "'%s'" %)))]]]])}
                                           ;    ; ))]]]])}
                                           ;    :cljs (gstring/format "'%s'")))]]]])}
                                           (#?(:clj format :cljs gstring/format) "'%s'"))]]]])}
                                           ; #(#?(:clj format :cljs gstring/format) "'%s'" %))]]]])}
                 ; WARN: Fixes most cljs tests, changes id in cljc
                 ; [:arguments [:string (->> beat last name #(#?(:clj format :cljs gstring/format) "'%s'"))]]]])}
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

(deftest durations
  (testing "reduction"
    (testing "set -> lists"
      (let [tree #{[2 2] [2 1 1]}
            tree-2 [:set
                   [:list
                    [:beat 2 [:identifier :scale-a]]
                    [:beat 2 [:identifier :scale-b]]]
                   [:list
                    [:beat 2 [:identifier :chord-a]]
                    [:beat 1 [:identifier :chord-b]]
                    [:beat 1 [:identifier :chord-c]]]]
            want 4
            actual (-> tree compose/reduce-durations)]
        (is (= want actual))))
    (testing "list -> sets"
      (let [tree [#{1 2} #{3 4} 5]
            want 11
            actual (-> tree compose/reduce-durations)]
        (is (= want actual))))))


; (testing "durations"
;   (testing "beats"
;     (let [tree [:beat
;                 [:number "3"]
;                 [:identifier :a]]
;           want 3]
;     (is (= want (compose/normalize-durations tree)))))
;   (testing "lists"
;     (let [tree [:list
;                 [:beat
;                   [:number "1"]
;                   [:identifier :a]]
;                 [:beat
;                   [:number "2"]
;                   [:identifier :b]]
;                 [:beat
;                   [:number "3"]
;                   [:identifier :c]]]
;           want 6]
;       (is (= want (compose/normalize-durations tree)))))
;   (testing "sets"
;     (let [tree [:set
;                 [:beat
;                   [:number "1"]
;                   [:identifier :a]]
;                 [:beat
;                   [:number "4"]
;                   [:identifier :b]]
;                 [:beat
;                   [:number "2"]
;                   [:identifier :c]]]
;           want 4]
;       (is (= want (compose/normalize-durations tree))))))


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
;           ; actual (compose/itemize-beats tree)]
;           actual (compose/itemize-beats tree)]
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

; (deftest quantize
(deftest transpose
  (with-redefs [compose/uid next-id!]
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
        (is (= want actual))))
    (testing "sets")))

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

(deftest quantize-collections
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
          actual (compose/quantize-collections tree 1/2)]
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
          actual (compose/quantize-collections tree 1/2)]
      (clojure.pprint/pprint actual)
      ; (clojure.pprint/pprint (-> actual bach.tree/squash))
      (is (= want actual))))
  (testing "list -> set -> beat"))
      ; (is (= false actual)))))

; FIXME: Need to test something like this:
; {
;   [1 -> scale('E lydian') 1 -> scale('E lydian')]
;   [1 -> chord('E') 1/2 -> chord('G#min') 1/2 -> chord('B')]
; }
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
      (clojure.pprint/pprint actual)
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
          actual (compose/normalize-beats tree (/ 1 2))]
      (is (= want actual)))))

; TODO: Remove or refactor based on new concurrent beat work
(deftest steps
  (with-redefs [compose/uid next-id!]
    (testing "play"
      (clear!)
      (let [tree (atomize-fixture fixture-a)
            actual (-> tree compose/normalize-beats compose/provision-play-steps)
            want [["stub.6OzHc6"]
                  ["stub.6mbq6m" "stub.z0Ntrz"]
                  nil
                  nil
                  ["stub.UO6vk1" "stub.0U44U0"]
                  nil
                  nil
                  nil
                  nil
                  nil
                  ["stub.cbw1Cc" "stub.Cubbb1" "stub.PzwAN0"]
                  nil
                  nil
                  nil
                  nil
                  nil
                  nil
                  nil]]
        ; (clojure.pprint/pprint actual)
        (is (= want actual))))
   (testing "stop"
     (testing "separate occurence"
       (clear!)
       (let [tree (atomize-fixture fixture-a)
             actual (-> tree compose/normalize-beats compose/provision-stop-steps)
             want [["stub.PzwAN0"]
                   ["stub.6OzHc6"]
                   nil
                   ["stub.6mbq6m"]
                   ["stub.z0Ntrz"]
                   nil
                   nil
                   nil
                   ["stub.UO6vk1"]
                   nil
                   ["stub.0U44U0"]
                   nil
                   nil
                   nil
                   nil
                   ["stub.Cubbb1"]
                   nil
                   ["stub.cbw1Cc"]]]
         (is (= want actual))))
     (testing "simultaneous occurence"
       (clear!)
       (let [tree (atomize-fixture fixture-b)
             actual (-> tree compose/normalize-beats compose/provision-stop-steps)
             want [["stub.cbw1Cc"]
                   ["stub.6OzHc6"]
                   nil
                   ["stub.6mbq6m"]
                   ["stub.z0Ntrz"]
                   nil
                   nil
                   nil
                   ["stub.UO6vk1" "stub.0U44U0"]
                   nil
                   nil
                   nil
                   nil
                   ["stub.Cubbb1"]]]
         (is (= want actual)))))
   (testing "step beats"
     (clear!)
     (let [tree (atomize-fixture fixture-a)
           actual (compose/provision-beat-steps tree)
           want [0 1 1 1 2 2 2 2 2 2 3 3 3 3 3 3 3 3]]
       (is (= want actual))))))

; FOCUS
(deftest steps-2
  (with-redefs [compose/uid (memoize next-id!)]
  ; (with-ids
    (testing "provisioned elements"
     (clear!)
     (println "!!!!!! counter" @id-counter)
     (let [;tree (atomize-fixture fixture-a)
           tree (atomize-fixture fixture-e)
           actual (bach.tree/cast-tree sequential? vec (compose/provision-element-steps tree 1/2))
           want [[["stub.3"] ["stub.5"] ["stub.1"]]
                 [["stub.3"] ["stub.5"] ["stub.1"]]
                 [["stub.6"] ["stub.3"] ["stub.1"]]
                 [["stub.6"] ["stub.3"] ["stub.1"]]
                 [["stub.2"] ["stub.6"] ["stub.3"]]
                 [["stub.2"] ["stub.6"] ["stub.3"]]
                 [["stub.4"] ["stub.2"] ["stub.6"]]
                 [["stub.4"] ["stub.2"] ["stub.6"]]
                 [["stub.4"] ["stub.2"] ["stub.6"]]
                 [["stub.4"] ["stub.2"] ["stub.6"]]]]
       (is (= want actual))))
    (testing "provisioned beats"
     (clear!)
     (let [tree (atomize-fixture fixture-e)
           actual (bach.tree/cast-tree sequential? vec (compose/provision-beat-steps-2 tree 1/2))
           want [0 0 1 1 2 2 3 3 3 3]]
       (println "DONE provision beats")
       (is (= want actual))))
    ; FIXME: Breaks if we remove memoize from next-id!
    (testing "provisioned states"
     ; (println "provision states clear???" @id-counter (next-id! nil))
     (clear!)
     (println " after clear???" @id-counter)
     (let [tree (atomize-fixture fixture-e)
           actual (bach.tree/cast-tree sequential? vec (compose/provision-state-steps tree 1/2))
           want [[0 ["stub.3"] ["stub.5"] ["stub.1"]]
                 [0 ["stub.3"] ["stub.5"] ["stub.1"]]
                 [1 ["stub.6"] ["stub.3"] ["stub.1"]]
                 [1 ["stub.6"] ["stub.3"] ["stub.1"]]
                 [2 ["stub.2"] ["stub.6"] ["stub.3"]]
                 [2 ["stub.2"] ["stub.6"] ["stub.3"]]
                 [3 ["stub.4"] ["stub.2"] ["stub.6"]]
                 [3 ["stub.4"] ["stub.2"] ["stub.6"]]
                 [3 ["stub.4"] ["stub.2"] ["stub.6"]]
                 [3 ["stub.4"] ["stub.2"] ["stub.6"]]]]
       (clear!)
       (clojure.pprint/pprint actual)
       (is (= want actual))))
    (testing "provisioned plays"
     (clear!)
     (let [tree (atomize-fixture fixture-e)
           actual (bach.tree/cast-tree sequential? vec (-> tree (compose/normalize-beats-2 1/2) (compose/provision-play-steps-2)))
           want [["stub.1" "stub.5" "stub.3"]
                 []
                 ["stub.6"]
                 []
                 ["stub.2"]
                 []
                 ["stub.4"]
                 []
                 []
                 []]]
       (is (= want actual))))
    (testing "provisioned stops"
     (clear!)
     (let [tree (atomize-fixture fixture-e)
           actual (-> tree (compose/normalize-beats-2 1/2) (compose/provision-stop-steps-2))
           want [["stub.4" "stub.2" "stub.6"]
                 nil
                 ["stub.5"]
                 nil
                 ["stub.1"]
                 nil
                 ["stub.3"]
                 nil
                 nil
                 nil]]
       (is (= want actual))))
    ; INPROG
    ; (testing "provisioned unified steps"
    ;   (clear!)
    ;   (let [tree (atomize-fixture fixture-e)
    ;         actual (-> tree (compose/provision-steps-2 1/2))
    ;         want []]
    ;     (is (= want actual))
    ;         ))))
    ))

(deftest provision
  (with-redefs [compose/uid next-id!]
    (testing "elements"
      (testing "all unique"
        (clear!)
        (let [tree (atomize-fixture fixture-a)
              actual (-> tree compose/normalize-beats compose/provision-elements)
              want {:stub
                    {"6OzHc6" {:value "a", :props ()},
                     "6mbq6m" {:value "b", :props ()},
                     "z0Ntrz" {:value "c", :props ()},
                     "0U44U0" {:value "f", :props ()},
                     "UO6vk1" {:value "d", :props ()},
                     "PzwAN0" {:value "h", :props ()},
                     "Cubbb1" {:value "e", :props ()},
                     "cbw1Cc" {:value "g", :props ()}}}]
          (is (= want actual))))
      (testing "some identical"
        (clear!)
        (let [tree (atomize-fixture fixture-c)
              actual (-> tree compose/normalize-beats compose/provision-elements)
              want {:stub
                    {"6OzHc6" {:value "a", :props ()},
                     "6mbq6m" {:value "b", :props ()}}}]
          (is (= want actual)))))
   (testing "beats"
      (clear!)
      (let [tree (atomize-fixture fixture-a)
            actual (-> tree compose/normalize-beats compose/provision-beats)
            want [{:items [{:duration 1, :elements ["stub.6OzHc6"]}],
                   :duration 1,
                   :index 0}
                  {:items
                   [{:duration 2, :elements ["stub.6mbq6m"]}
                    {:duration 3, :elements ["stub.z0Ntrz"]}],
                   :duration 3,
                   :index 1}
                  {:items
                   [{:duration 4, :elements ["stub.UO6vk1"]}
                    {:duration 6, :elements ["stub.0U44U0"]}],
                   :duration 6,
                   :index 4}
                  {:items
                   [{:duration 5, :elements ["stub.Cubbb1"]}
                    {:duration 7, :elements ["stub.cbw1Cc"]}
                    {:duration 8, :elements ["stub.PzwAN0"]}],
                   :duration 8,
                   :index 10}]]
        (is (= (norm want) actual))))))

; (clojure.pprint/pprint (->> fixture-a atomize-fixture (conj [:play]) compose/get-play))
; (clojure.pprint/pprint (-> fixture-a atomize-fixture compose/playable))
; (clojure.pprint/pprint (-> fixture-a atomize-fixture compose/compose))
; (clojure.pprint/pprint (-> fixture-a atomize-fixture compose/parse))
; (clojure.pprint/pprint (-> fixture-a atomize-fixture compose/provision))
; (clojure.pprint/pprint (-> [:loop [:number "2"] [:list [:string "'a'"] [:string "'z'"]]] track/reduce-values compose/normalize-loops))

; (println "@@@@@@@@@@@")
; (clojure.pprint/pprint (-> [:list [:play [:a 1]] [:play [:b 2]]] (compose/get-play)))

(def fixture-bach-a
  "
  :a = chord('A7')
  :e = chord('E6')
  :g = chord('Gm')
  :f = chord('F#')

  :part-a = 3 of [
    3 -> { :a, scale('A dorian'), _ }
    2 -> :e
    when 3 do { 1 -> :g }
  ]

  :part-b = 2 of [
    when 1 do {
      2 -> :a
      2 -> chord('Z')
    }
    1 -> :f
    1 -> :e
  ]

  ## Look a comment

  play! [:part-a :part-b]
  ## play! 2 of [:part-a :part-b]
  ")

(println "@@@@@@")
; (clojure.pprint/pprint (compose/compose fixture-bach-a))
; (clojure.pprint/pprint (-> fixture-bach-a compose/get-play compose/reduce-track))
; (clojure.pprint/pprint (-> fixture-bach-a bach.ast/parse))
; (clojure.pprint/pprint (-> fixture-bach-a bach.ast/parse track/reduce-values))
; (clojure.pprint/pprint (-> fixture-bach-a bach.ast/parse compose/digest))
; (clojure.pprint/pprint (-> fixture-bach-a bach.ast/parse compose/provision))
; (clojure.pprint/pprint (-> fixture-bach-a
;                            bach.ast/parse
;                            compose/reduce-track
;                            compose/get-play
;                            compose/normalize-beats))
                           ; compose/normalize-collections)); compose/itemize-beats))
; (clojure.pprint/pprint (-> fixture-bach-a compose/compose))
; (clojure.pprint/pprint (-> fixture-bach-a bach.ast/parse compose/digest compose/validate))
; (clojure.pprint/pprint (-> fixture-bach-a bach.ast/parse compose/parse compose/get-iterations))
; (clojure.pprint/pprint (-> fixture-bach-a bach.ast/parse compose/digest compose/reduce-iterations))
; (clojure.pprint/pprint (-> fixture-bach-a bach.ast/parse compose/digest compose/reduce-iterations))

; (clojure.pprint/pprint (bach.ast/parse fixture-bach-a))
; (clojure.pprint/pprint (bach.ast/parse "[1 -> :a, 2 -> :b]"))
; (clojure.pprint/pprint (bach.ast/parse "[when 1, when 2]"))
; (clojure.pprint/pprint (bach.ast/parse "[when 1 then [ 1 -> :a ], when 2 then { 2 -> :b }]"))
; (clojure.pprint/pprint (bach.ast/parse "[when 1 then [ 1 -> :a ]]"))
; (clojure.pprint/pprint (bach.ast/parse "[when 1 then :a]"))

; (println "---- new beat steps")
; (clojure.pprint/pprint (-> fixture-e (compose/itemize-beats-2 1/2)))
; (clojure.pprint/pprint (-> fixture-e (compose/provision-beat-steps-2 1/2)))
; (clojure.pprint/pprint (-> fixture-e (compose/provision-context-steps 1/2)))
; (clojure.pprint/pprint (-> fixture-e atomize-fixture (compose/normalize-beats-2 1/2) (compose/provision-play-steps-2)))
; (clojure.pprint/pprint (-> fixture-e atomize-fixture (compose/itemize-beats-2 1/2) (compose/provision-stop-steps-2)))
; (clojure.pprint/pprint (-> fixture-e atomize-fixture (compose/itemize-beats-2 1/2) (compose/provision-event-steps)))
; (clojure.pprint/pprint (-> fixture-e atomize-fixture (compose/provision-steps-2 1/2)))

; BORKED
; (clojure.pprint/pprint (-> fixture-bach-a bach.track/playable (compose/normalize-beats-2 1/2)))
; BORKED
; (clojure.pprint/pprint (-> fixture-bach-a bach.track/playable (compose/quantize-collections 1/2)))
; WORKS
; (clojure.pprint/pprint (-> fixture-bach-a bach.track/playable compose/normalize-collections))
; WORKS
; (clojure.pprint/pprint (-> fixture-bach-a bach.track/playable compose/normalize-collections (compose/unitize-durations 1/2)))
; BREAKS (issue is with tranpose-lists)
;  - FIXED: After calling recursive transpose-lists for sequential items
(println "\n\n-=-=-=-=-=-=-=-=-=\n\n")
; (clojure.pprint/pprint (-> fixture-bach-a bach.track/playable compose/normalize-collections (compose/unitize-durations 1) compose/transpose-sets compose/transpose-lists))
; NOTE: Using transpose-sets (and not transpose-lists) looks more like what we want
; (clojure.pprint/pprint (-> fixture-bach-a bach.track/playable compose/normalize-collections (compose/unitize-durations 1) compose/transpose-sets)) ;compose/transpose-sets))

; (clojure.pprint/pprint (-> fixture-bach-a bach.track/playable (compose/normalize-beats-2 1))) ;compose/transpose-sets))

; (clojure.pprint/pprint (-> fixture-bach-a bach.track/playable compose/normalize-collections))
; (clojure.pprint/pprint (-> fixture-bach-a bach.track/playable (compose/itemize-beats-2 1))) ;compose/transpose-sets))
; (clojure.pprint/pprint (-> fixture-bach-a bach.track/playable compose/normalize-collections (compose/unitize-durations 1) compose/transpose-sets))
; (clojure.pprint/pprint (-> fixture-bach-a bach.track/playable (compose/normalize-beats-2 1)))
; LAST
; (clojure.pprint/pprint (-> fixture-bach-a compose/compose))

; (clojure.pprint/pprint (-> fixture-e (compose/itemize-beats-2 1/2) compose/beat-as-items))
; (let [beat-steps (-> fixture-e (compose/provision-beat-steps-2 1/2))
;       elem-steps (-> fixture-e (compose/provision-element-steps 1/2))]
;   (clojure.pprint/pprint
;     (map cons beat-steps elem-steps)))
    ; (map #(cons %1 %2) beat-steps elem-steps)))

; (let [beat-steps (-> fixture-e atomize-fixture (compose/normalize-beats-2 1/2) compose/provision-play-steps-2)
;       elem-steps (-> fixture-e atomize-fixture (compose/normalize-beats-2 1/2) compose/provision-stop-steps-2)]
;   (clojure.pprint/pprint
;     ; (map cons beat-steps elem-steps)))
;     (map (partial conj []) beat-steps elem-steps)))
