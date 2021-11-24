(ns qbits.flex.interceptor
  (:require [qbits.flex.protocols :as p]
            [qbits.flex.ex :as ex]))

(defn interceptor
  "Interceptor of `limiter` and `sampler` that will compute rtt avg for
  handler and reject requests past limited threshold"
  [{:keys [limiter]}]
  {:enter (fn [ctx]
            (let [request (p/acquire! limiter)]
              (cond-> (assoc ctx
                             :qbits.flex/request request)
                (not (p/accepted? request))
                (assoc :exoscale.interceptor/error (ex/ex-rejected @request)))))

   :leave (fn [{:as ctx :qbits.flex/keys [request]}]
            (when (p/accepted? request)
              (p/complete! request))
            ctx)

   :error (fn [{:qbits.flex/keys [request]} err]
            (if (p/accepted? request)
              (p/complete! request)
              (p/reject! request))
            (throw err))})
