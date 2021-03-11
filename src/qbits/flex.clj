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
  [clock sampler recorder limit ; deps
   accept hooks]
  (let [in-flight (p/-inc! recorder)
        current-limit @limit
        start-time @clock
        accepted (accept in-flight current-limit)
        status (promise)
        {:qbits.flex.hooks/keys [complete ignore drop]
         :or {complete identity
              ignore identity
              drop identity}} hooks]
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
        (complete this)
        (deliver status ::completed))

      (-ignore! [this]
        (p/dec! recorder)
        (ignore this)
        (deliver status ::ignored))

      (-drop! [this]
        (p/dec! recorder)
        (p/update! limit
                   @sampler
                   in-flight
                   true)
        (drop this)
        (deliver status ::dropped))

      clojure.lang.IDeref
      (deref [_]
        {::current-limit current-limit
         ::start-time start-time
         ::accepted? accepted
         ::sample @sampler}))))

(defn limiter
  [{:keys [clock sampler limit recorder accept hooks]
    :or {recorder (recorder)
         clock (clock)
         sampler (sampler/make)
         accept (fn [in-flight current-limit]
                  (<= in-flight current-limit))}}]
  (reify p/Limiter
    (-acquire! [this]
      (request clock
               sampler
               recorder
               limit
               accept
               hooks))

    clojure.lang.IDeref
    (deref [_]
      {:in-flight @recorder
       :limit @limit
       :sample @sampler})))

(defn quota-limiter
  "Like `limiter` but will limit the requests to :quota, defaults to
  1.0 (100%)"
  [{:as opts :keys [quota] :or {quota 1.0}}]
  (limiter (assoc opts :accept
                  (fn [in-flight current-limit]
                    (<= in-flight (* quota current-limit))))))
