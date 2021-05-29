; (ns bach.core-test
;   ; (:require #?(:cljs ["@peculiar/webcrypto" :refer [Crypto]])))
;   (:require ["@peculiar/webcrypto" :refer [Crypto]]
;             [eftest.runner :refer [find-tests run-tests]]))

; ; #?(:cljs (set! js/crypto (Crypto.)))
; (set! js/crypto (Crypto.))

; (defn -main []
;   (run-tests (find-tests "test") {:multithread? false}))
