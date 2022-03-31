(ns s-exp.flex.middleware
  (:require [s-exp.flex.protocols :as p]
            [s-exp.flex.ex :as ex]))

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
