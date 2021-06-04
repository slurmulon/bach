(ns bach.crypto
  (:require ["@peculiar/webcrypto" :refer [Crypto]]))

(set! js/crypto (Crypto.))
