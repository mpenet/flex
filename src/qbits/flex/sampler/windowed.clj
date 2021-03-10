(ns qbits.flex.sampler.windowed
  (:require [qbits.flex.protocols :as p]))

;; simple sampler that records a moving window of rtt-length
(def defaults {::length 100})

(defn make
  [{::keys [length initial]
    :or {length 100
         initial nil}}]
  (let [state (atom {::val initial})]
    (reify p/Sampler
      (-sample! [_ val]
        (-> (swap! state
                   update
                   ::val
                   (fnil #(-> %
                              (* (dec length))
                              (+ val)
                              (/ length)
                              (long))
                         val))
            ::val))
      clojure.lang.IDeref
      (deref [_] (::val @state)))))
