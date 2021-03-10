(ns qbits.flex.limit.aimd
  (:require [qbits.flex.protocols :as p]))

(def defaults
  {::initial-limit 20
   ::min-limit 20
   ::max-limit 200
   ::backoff-ratio 0.9
   ::inc-after (fn [in-flight] (* 2 in-flight))
   ;; 5s in ns
   ::timeout (* 5 1e9)})

(defn make
  ([] (make {}))
  ([opts]
   (let [{:as _opts
          ::keys [initial-limit backoff-ratio timeout max-limit min-limit
                  inc-after]}
         (merge defaults opts)
         state (atom {:limit initial-limit})]
     (reify
       clojure.lang.IDeref
       (deref [_] (:limit @state))

       p/Limit
       (-state [_] @state)

       (-add-watch! [_ k f]
         (add-watch state
                    k
                    (fn [_k _r
                         {old-limit :limit}
                         {new-limit :limit}]
                      (when (not= old-limit
                                  new-limit)
                        (f new-limit)))))

       (-update! [_ rtt in-flight dropped?]
         (-> (swap! state
                    (fn [{:keys [limit] :as state}]
                      (assoc state
                             :limit
                             (let [limit (cond
                                           (or dropped? (> rtt timeout))
                                           (long (* limit backoff-ratio))

                                           (>= (inc-after in-flight) limit)
                                           (inc limit)

                                           :else limit)]
                               (min max-limit
                                    (max min-limit limit))))))
             :limit))))))
