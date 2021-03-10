(ns qbits.flex.limiter.simple
  (:require [qbits.flex.protocols :as p]
            [exoscale.ex :as ex]))

(defn make
  ([] (make {}))
  ([_opts]
   (reify p/Limiter
     (-acquire! [_ current-limit in-flight extra]
       (when (>= in-flight current-limit)
         (throw (ex/ex-info "Request rejected"
                            [:qbits.flex/rejected [:exoscale.ex/unavailable]]
                            (merge {:limit current-limit
                                    :current-in-flight in-flight}
                                   extra)))))
     (-acquire! [this current-limit in-flight]
       (p/-acquire! this current-limit in-flight {})))))
