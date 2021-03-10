(ns qbits.flex.middleware
  (:require [qbits.flex.protocols :as p]
            [exoscale.ex :as ex]))

(def back-off-response {:status 420 :body "enhance your calm"})

(defn with-limiter
  [handler {:keys [limiter]}]
  (fn [request]
    (if-let [start-time (:time (p/acquire! limiter))]
      (try (handler request)
           (finally
             (p/complete! limiter start-time)))
      (do
        (p/reject! limiter)
        back-off-response))))
