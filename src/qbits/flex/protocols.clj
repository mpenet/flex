(ns qbits.flex.protocols
  (:refer-clojure :exclude [add-watch!]))

(defprotocol Limit
  (-state [this])
  (-add-watch! [this k f])
  (-update! [this rtt in-flight dropped?]))

(defprotocol Limiter
  (-acquire!
    [this current-limit in-flight]
    [this current-limit in-flight extra]))

(defprotocol Sampler
  (-sample! [this val] "Records avg rtts for context"))

(defprotocol Recorder
  (-inc! [this])
  (-dec! [this]))

(defprotocol Clock
  (-duration [this start-time]))

(def state -state)
(def update! -update!)
(def add-watch! -add-watch!)
(def acquire! -acquire!)
(def sample! -sample!)
(def inc! -inc!)
(def dec! -dec!)
(def duration -duration)
