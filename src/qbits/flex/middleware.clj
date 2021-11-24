(ns qbits.flex.middleware
  (:require [qbits.flex.protocols :as p]
            [qbits.flex.ex :as ex]))

(defn with-limiter
  [handler {:keys [limiter]}]
  (fn [req-map]
    (let [request (p/acquire! limiter)]
      (if (p/accepted? request)
        (try (handler req-map)
             (finally
               (p/complete! request)))
        (do (p/reject! request)
            (ex/ex-rejected! @request))))))
