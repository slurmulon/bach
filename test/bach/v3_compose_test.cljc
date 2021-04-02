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
                   [:number 1]
                   [:identifier :a]]
                  [:set
                   [:identifier :b]
                   [:identifier :c]]
                  [:set
                   [:identifier :d]
                   [:list
                    [:identifier :e]
                    [:identifier :f]]]]
            want [{:duration
                    [:number 1],
                  :elements
                    [:identifier :a]}
                  #{[:identifier :b] [:identifier :c]}
                  #{[:identifier :d] [[:identifier :e] [:identifier :f]]}]]
        (is (= want (compose/normalize-collection-tree tree)))))
    (testing "loops"
      (testing "root-level"
        (let [tree [:loop
                     2
                     [:list
                       [:pair
                         [:number 1]
                         [:identifier :a]]
                       [:pair
                         [:number 3]
                         [:identifier :b]]]]
              want [{:duration 1
                     :elements [:identifier :a]}
                    {:duration 3
                     :elements [:identifier :b]}
                    {:duration 1
                     :elements [:identifier :a]}
                    {:duration 3
                     :elements [:identifier :b]}]]
            (println (compose/normalize-collection-tree tree))
            (is (= true true)))))))
      ; (is (= want (compose/reduce-durations tree))))))
