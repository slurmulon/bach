(ns bach.v3-compose-test
  (:require #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer-macros [deftest is testing run-tests]])
            [bach.compose :as compose]
            [bach.data :refer [to-ratio]]))

; @see https://cljdoc.org/d/leiningen/leiningen/2.9.5/api/leiningen.test
; (deftest ^:v3 normalization
(deftest normalization
  (testing "collection-tree"
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
        (is (= want (compose/normalize-collection-tree tree)))))
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
            (is (= want (compose/normalize-collection-tree tree)))))
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
              (is (= want (compose/normalize-collection-tree tree)))))))))

; (deftest reduction
;   (testing "durations"
;     (testing "beats"
;       (let [tree [:list
;                   [:pair
      ; (is (= want (compose/reduce-durations tree))))))
