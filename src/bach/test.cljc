; (ns bach.test
;   (:require #?(:cljs ["@peculiar/webcrypto" :refer [Crypto]])
;   ; (:require ["@peculiar/webcrypto" :refer [Crypto]]
;               [cljs.test :refer-macros [run-tests]]))
;             ; [eftest.runner :refer [find-tests run-tests]]))

; #?(:cljs (set! js/crypto (Crypto.)))
; ; (set! js/crypto (Crypto.))

; (defn -main []
;   ; (run-tests (find-tests "test") {:multithread? false}))
;   (run-tests 'bach.ast-test
;              'bach.v3-compose-test
;              'bach.v3-integration-test))
