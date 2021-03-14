(ns qbits.flex
  (:require [qbits.flex.protocols :as p]
            [qbits.flex.sampler.windowed :as sampler])
  (:import (java.util.concurrent.atomic AtomicLong)))

(defn recorder
  ([] (recorder {}))
  ([{::keys [initial]
     :or {initial 0}}]
   (let [atm (AtomicLong. initial)]
     (reify p/Recorder
       (-inc! [_] (.incrementAndGet atm))
       (-dec! [_] (.decrementAndGet atm))
       clojure.lang.IDeref
       (deref [_] (.get atm))))))

(defn clock
  ([] (clock {}))
  ([_opts]
   (reify p/Clock
     (-duration [_ t] (- (System/nanoTime) t))
     clojure.lang.IDeref
     (deref [_] (System/nanoTime)))))

(defn request
  [{:keys [clock sampler recorder limit ; deps
           accept]
    :as limiter}]
  (let [in-flight (p/-inc! recorder)
        current-limit @limit
        start-time @clock
        accepted (accept in-flight current-limit)
        {:qbits.flex.hooks/keys [complete ignore drop]
         :or {complete identity
              ignore identity
              drop identity}} limiter]
    (reify p/Request
      (-accepted? [_] accepted)
      (-rejected? [_] (not accepted))

      (-complete! [this]
        (p/-dec! recorder)
        (p/update! limit
                   (p/sample! sampler
                              (p/duration clock
                                          start-time))
                   in-flight
                   false)
        (complete this))

      (-ignore! [this]
        (p/dec! recorder)
        (ignore this))

      (-drop! [this]
        (p/dec! recorder)
        (p/update! limit
                   @sampler
                   in-flight
                   true)
        (drop this))

      clojure.lang.IDeref
      (deref [_]
        {::current-limit current-limit
         ::start-time start-time
         ::accepted? accepted
         ::sample @sampler}))))

(defn- limiter-defaults
  [{:keys [clock sampler _limit recorder accept] :as opts}]
  (cond-> opts
    (not recorder)
    (assoc :recorder (qbits.flex/recorder))

    (not sampler)
    (assoc :sampler (sampler/make))

    (not clock)
    (assoc :clock (qbits.flex/clock))

    (not accept)
    (assoc :accept
           (fn [in-flight current-limit]
             (<= in-flight current-limit)))))

(defrecord Limiter [clock sampler limit recorder accept hooks]
  p/Limiter
  (-acquire! [this]
    (request this))

  clojure.lang.IDeref
  (deref [_]
    {:in-flight @recorder
     :limit @limit
     :sample @sampler}))

(defn limiter
  [{:keys [clock sampler limit recorder accept] :as opts}]
  (map->Limiter (limiter-defaults opts)))

(defn quota-limiter
  "Like `limiter` but will limit the requests to :quota, defaults to
  1.0 (100%)"
  [{:as opts :keys [quota] :or {quota 1.0}}]
  (limiter (assoc opts :accept
                  (fn [in-flight current-limit]
                    (<= in-flight (* quota current-limit))))))
