(ns qbits.flex.protocols
  (:refer-clojure :exclude [time]))

(defprotocol Limit
  (-state [this])
  (-watch-limit! [this k f])
  (-update! [this rtt in-flight dropped?]))

(defprotocol Limiter
  (-acquire! [this])
  (-complete! [this start-time])
  (-reject! [this])
  (-ignore! [this])

  (-in-flight [this])
  (-sample [this])
  (-time [this]))

(defprotocol Sampler
  (-sample! [this val] "Records avg rtts for context"))

(defprotocol Recorder
  (-inc! [this])
  (-dec! [this]))

(defprotocol Clock
  (-duration [this start-time]))

(def state -state)
(def update! -update!)
(def watch-limit! -watch-limit!)
(def acquire! -acquire!)
(def sample! -sample!)
(def inc! -inc!)
(def dec! -dec!)
(def duration -duration)

(def complete! -complete!)
(def reject! -reject!)
(def ignore! -ignore!)
(def sample -sample)
(def time -time)
