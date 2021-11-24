(ns qbits.flex
  (:require [qbits.flex.protocols :as p]
            [qbits.flex.limit.aimd :as aimd]
            [qbits.flex.sampler.windowed :as sampler])
  (:import (java.util.concurrent.atomic AtomicLong)))

(defn counter
  ([{:keys [initial]
     :or {initial 0}}]
   (let [atm (AtomicLong. initial)]
     (reify p/Counter
       (-inc! [_] (.incrementAndGet atm))
       (-dec! [_] (.decrementAndGet atm))
       clojure.lang.IDeref
       (deref [_] (.get atm))))))

(defn clock
  ([_opts]
   (reify p/Clock
     (-duration [_ t] (- (System/currentTimeMillis) t))
     clojure.lang.IDeref
     (deref [_] (System/currentTimeMillis)))))

(defn- noop [& _])

(defn request
  [{:keys [clock sampler counter limit ; deps
           accept? dropped?]
    :as limiter}]
  (let [current-limit @limit
        in-flight (p/-inc! counter)
        start-time @clock
        ;; whether we're still within acceptable limits
        accepted (accept? in-flight current-limit)
        {:qbits.flex.hooks/keys [reject complete]
         :or {reject noop complete noop}} limiter]
    (reify p/Request
      (-accepted? [_] accepted)

      (-complete! [this]
        (let [rtt (p/duration clock start-time)
              in-flight @counter
              [old-rtt new-rtt] (p/sample! sampler rtt)
              ;; "drop" as in avg drop, not request drop
              dropped (dropped? old-rtt new-rtt)]
          (p/update! limit
                     rtt
                     in-flight
                     dropped)
          (p/-dec! counter)
          (complete this rtt in-flight dropped)))

      (-reject! [this]
        (p/dec! counter)
        (reject this))

      clojure.lang.IDeref
      (deref [_]
        {::current-limit current-limit
         ::start-time start-time
         ::accepted? accepted
         ::sample @sampler}))))

(defn- limiter-defaults
  [{:keys [clock sampler limit counter quota accept? dropped?]
    :or {quota 1.0}
    :as opts}]
  (cond-> opts
    (not limit)
    (assoc :limit (aimd/make opts))
    (not counter)
    (assoc :counter (qbits.flex/counter opts))

    (not sampler)
    (assoc :sampler (sampler/make opts))

    (not clock)
    (assoc :clock (qbits.flex/clock opts))

    (not accept?)
    (assoc :accept?
           (fn accept? [in-flight current-limit]
             (<= in-flight (* quota current-limit))))

    (not dropped?)
    (assoc :dropped?
           (fn dropped? [old-rtt new-rtt]
             (> new-rtt old-rtt)))))

(defrecord Limiter [clock sampler limit counter accept? hooks]
  p/Limiter
  (-acquire! [this] (request this))
  clojure.lang.IDeref
  (deref [_]
    {:in-flight @counter
     :limit @limit
     :sample @sampler}))

(defn limiter
  [opts]
  (map->Limiter (limiter-defaults opts)))
