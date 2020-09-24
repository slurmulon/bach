; (ns bach.smoke-test
;   (:require [clojure.test :refer :all]
;             [bach.track :refer :all]
;             [bach.ast :refer :all]))

; (deftest "long and diverse tracks"
;   (deftest "1."
;     (let [tree (parse "@Tempo = 91

; !Play [
;   9 -> {
;     Scale('F# dorian')
;     Chord('F#m')
;   }
;   1 -> Chord('C#m')
;   1 -> Chord('F#m')
;   1 -> Chord('F#m6')
;   1 -> Chord('F#m')
;   1 -> Chord('C#m')
;   3 -> Chord('F#m')

;   1 -> Chord('C#m')
;   1 -> Chord('F#m')
;   1 -> Chord('F#m6')
;   1 -> Chord('F#m')
;   1 -> Chord('C#m')
;   2 -> Chord('F#m')

;   3/16 -> Chord('C#m')
;   3/16 -> Chord('F#m')
;   10/16 -> Chord('G#m')

;   3/16 -> Chord('C#m')
;   3/16 -> Chord('F#m')
;   10/16 -> Chord('G#m')

;   3/16 -> Chord('A')
;   3/16 -> Chord('G#m')
;   10/16 -> Chord('F#m')
;   2 -> Chord('F#m')
; ]")
;           want {:headers
;  {:tags [],
;   :desc "",
;   :time [4 4],
;   :total-beats 29N,
;   :title "Untitled",
;   :link "",
;   :ms-per-beat 164.83516,
;   :lowest-beat 1/16,
;   :audio "",
;   :tempo 91},
;  :data
;  [[{:duration 9,
;     :notes
;     ({:atom {:keyword "Scale", :init {:arguments ["F# dorian"]}}}
;      {:atom {:keyword "Chord", :init {:arguments ["F#m"]}}})}
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil]
;   [nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil]
;   [nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil]
;   [nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil]
;   [nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil]
;   [nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil]
;   [nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil]
;   [nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil]
;   [nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil]
;   [{:duration 1,
;     :notes {:atom {:keyword "Chord", :init {:arguments ["C#m"]}}}}
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil]
;   [{:duration 1,
;     :notes {:atom {:keyword "Chord", :init {:arguments ["F#m"]}}}}
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil]
;   [{:duration 1,
;     :notes {:atom {:keyword "Chord", :init {:arguments ["F#m6"]}}}}
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil]
;   [{:duration 1,
;     :notes {:atom {:keyword "Chord", :init {:arguments ["F#m"]}}}}
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil]
;   [{:duration 1,
;     :notes {:atom {:keyword "Chord", :init {:arguments ["C#m"]}}}}
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil]
;   [{:duration 3,
;     :notes {:atom {:keyword "Chord", :init {:arguments ["F#m"]}}}}
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil]
;   [nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil]
;   [nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil]
;   [{:duration 1,
;     :notes {:atom {:keyword "Chord", :init {:arguments ["C#m"]}}}}
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil]
;   [{:duration 1,
;     :notes {:atom {:keyword "Chord", :init {:arguments ["F#m"]}}}}
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil]
;   [{:duration 1,
;     :notes {:atom {:keyword "Chord", :init {:arguments ["F#m6"]}}}}
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil]
;   [{:duration 1,
;     :notes {:atom {:keyword "Chord", :init {:arguments ["F#m"]}}}}
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil]
;   [{:duration 1,
;     :notes {:atom {:keyword "Chord", :init {:arguments ["C#m"]}}}}
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil]
;   [{:duration 2,
;     :notes {:atom {:keyword "Chord", :init {:arguments ["F#m"]}}}}
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil]
;   [nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil]
;   [{:duration 3/16,
;     :notes {:atom {:keyword "Chord", :init {:arguments ["C#m"]}}}}
;    nil
;    nil
;    {:duration 3/16,
;     :notes {:atom {:keyword "Chord", :init {:arguments ["F#m"]}}}}
;    nil
;    nil
;    {:duration 5/8,
;     :notes {:atom {:keyword "Chord", :init {:arguments ["G#m"]}}}}
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil]
;   [{:duration 3/16,
;     :notes {:atom {:keyword "Chord", :init {:arguments ["C#m"]}}}}
;    nil
;    nil
;    {:duration 3/16,
;     :notes {:atom {:keyword "Chord", :init {:arguments ["F#m"]}}}}
;    nil
;    nil
;    {:duration 5/8,
;     :notes {:atom {:keyword "Chord", :init {:arguments ["G#m"]}}}}
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil]
;   [{:duration 3/16,
;     :notes {:atom {:keyword "Chord", :init {:arguments ["A"]}}}}
;    nil
;    nil
;    {:duration 3/16,
;     :notes {:atom {:keyword "Chord", :init {:arguments ["G#m"]}}}}
;    nil
;    nil
;    {:duration 5/8,
;     :notes {:atom {:keyword "Chord", :init {:arguments ["F#m"]}}}}
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil]
;   [{:duration 2,
;     :notes {:atom {:keyword "Chord", :init {:arguments ["F#m"]}}}}
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil
;    nil]
;   [nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil]]}]
;   (is (= want (compile-track tree))))))
