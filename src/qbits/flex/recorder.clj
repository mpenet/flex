(ns qbits.flex.recorder
  (:require [qbits.flex.protocols :as p])
  (:import (java.util.concurrent.atomic AtomicLong)))

(defn make
  ([] (make {}))
  ([{::keys [initial]
     :or {initial 0}}]
   (let [atm (AtomicLong. initial)]
     (reify p/Recorder
       (-inc! [_] (.incrementAndGet atm))
       (-dec! [_] (.decrementAndGet atm))
       clojure.lang.IDeref
       (deref [_] (.get atm))))))
