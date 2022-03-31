(ns s-exp.flex.protocols
  (:refer-clojure :exclude [time]))

(defprotocol Limit
  (-state [this]
    "Returns limit current state")
  (-watch-limit! [this k f]
    "Adds watch `f` on limit changes for key `k`")
  (-update! [this rtt in-flight dropped?]
    "Updates limit state for current request `rtt`, number of `in-flight` requests and potentially `dropped?` status"))

(defprotocol Limiter
  (-acquire! [this] "Attempts to acquire Request"))

(defprotocol Request
  (-accepted? [this] "Returns true if the requests was accepted")
  (-complete! [this] "Mark the current request as complete, updates sample/limit")
  (-reject! [this] "Marks the request as rejectd"))

(defprotocol Sampler
  (-sample! [this val] "Records avg rtts for context"))

(defprotocol Counter
  (-inc! [this] "Increases in-flight requests count")
  (-dec! [this] "Decreases in-flight requests count"))

(defprotocol Clock
  (-duration [this start-time]
    "Returns duration from `start-time` to `now` in nanosecs"))

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
(def accepted? -accepted?)
