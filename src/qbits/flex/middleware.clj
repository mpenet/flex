(ns qbits.flex.middleware
  (:require [qbits.flex.protocols :as p]
            [exoscale.ex :as ex]))

(def back-off-response {:status 420 :body "enhance your calm"})

(defn with-limiter
  [handler {:keys [limiter limit sampler recorder clock]}]
  (fn [request]
    (ex/try+
     (p/acquire! limiter
                 @limit
                 @recorder
                 {:limit limit
                  :clock clock
                  :sampler sampler})
     (p/inc! recorder)
     (let [start-time @clock]
       (try (handler request)
            (finally
              (p/update! limit
                         (p/sample! sampler
                                    (p/duration clock
                                                start-time))
                         (p/dec! recorder)
                         false))))

     (catch :qbits.flex/rejected _
       (p/update! limit
                  @sampler
                  @recorder
                  true)
       back-off-response))))
