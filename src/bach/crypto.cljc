(ns bach.crypto
  (:require #?(:cljs ["@peculiar/webcrypto" :refer [Crypto]])))

#?(:cljs (set! js/crypto (Crypto.)))
