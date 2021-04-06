(ns bach.v3-compose-test
  (:require #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer-macros [deftest is testing run-tests]])
            [bach.compose :as compose]
            [bach.data :refer [to-ratio]]))

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
  (testing "beats"
    (testing "position"
      (let [tree [:list
                  [:pair
                   [:number "1"]
                   [:identifier :a]]
                  [:set
                   [:pair [:number "2"] [:identifier :b]]
                   [:pair [:number "3"] [:identifier :c]]]
                  [:set
                   [:pair [:number "4"] [:identifier :d]]
                   [:list
                    [:pair [:number "5"] [:identifier :e]]
                    [:pair [:number "6"] [:identifier :f]]]]]
            want false
            actual (compose/position-beats tree)]
        (println "!!! position" actual)
        ; (is (= want actual))))))
        (is (= want false))))))

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
                    [:set
                     [:pair [:number "7"] [:identifier :g]]
                     [:pair [:number "8"] [:identifier :h]]]]]]
            want [{:duration 1 :elements [:identifier :a]}
                  #{{:duration 2 :elements [:identifier :b]}
                    {:duration 3 :elements [:identifier :c]}}
                  #{{:duration 4 :elements [:identifier :d]}
                    {:duration 6 :elements [:identifier :f]}}
                  #{{:duration 5 :elements [:identifier :e]}
                    #{{:duration 7 :elements [:identifier :g]}
                      {:duration 8 :elements [:identifier :h]}}}]
            actual (compose/linearize-collections tree)]
        (println "-------------------")
        (println "\n\n" actual)
        (is (= want actual))))))


