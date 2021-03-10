(ns qbits.flex.interceptor
  (:require [qbits.flex.protocols :as p]
            [exoscale.ex :as ex]))

(defn interceptor
  "Interceptor of `limiter` and `sampler` that will compute rtt avg for
  handler and reject requests past limited threshold"
  [{:keys [recorder sampler limiter limit clock]}]
  {:enter (fn [{:as ctx}]
            ;; this could throw so current-in-flight will only inc
            ;; if not rejected
            (p/acquire! limiter
                        @limit
                        @recorder
                        {:limit limit
                         :sampler sampler})
            (assoc ctx
                   ::current-in-flight (p/inc! recorder)
                   ::start-time @clock))

   :leave (fn [{:as ctx ::keys [start-time]}]
            (let [rtt-avg (p/sample! sampler
                                     (p/duration clock start-time))]
              (p/update! limit
                         rtt-avg
                         (p/dec! recorder)
                         false))
            ctx)

   :error (fn [{::keys [start-time]} err]
            ;; only decrease if it's bubbling from a real ex (not rejection)
            (if-not (ex/type? err :qbits.flex/rejected)
              (p/update! limit
                         (p/sample! sampler
                                    (p/duration clock start-time))
                         (p/dec! recorder)
                         false)
              ;; just register the drop
              (p/update! limit
                         @sampler
                         @recorder
                         true))
            (throw err))})
