(ns qbits.flex.interceptor
  (:require [qbits.flex.protocols :as p]
            [exoscale.ex :as ex]))

(defn interceptor
  "Interceptor of `limiter` and `sampler` that will compute rtt avg for
  handler and reject requests past limited threshold"
  [{:keys [limiter]}]
  {:enter (fn [{:as ctx}]
            ;; this could throw so current-in-flight will only inc
            ;; if not rejected
            (if-let [{:keys [time in-flight current-limit]} (p/acquire! limiter)]
              (assoc ctx
                     ::current-limit current-limit
                     ::current-in-flight in-flight
                     ::start-time time)
              (do
                ;; (p/ignore! limiter)
                (throw (ex-info "Rejected" {})))))

   :leave (fn [{:as ctx ::keys [start-time]}]
            (p/complete! limiter start-time)
            ctx)

   :error (fn [{::keys [start-time]} err]
            ;; only decrease if it's bubbling from a real ex (not rejection)
            (if start-time
              (p/complete! limiter start-time)
              ;; just register the drop
              (p/reject! limiter))
            (throw err))})
