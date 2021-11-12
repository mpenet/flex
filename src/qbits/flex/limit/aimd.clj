(ns qbits.flex.limit.aimd
  "Implementation of AIMD Limit,
  https://en.wikipedia.org/wiki/Additive_increase/multiplicative_decrease

  By default it will increase by 1 on low avg rtt and backoff by 0.9
  on elevated/ing latency, both functions of current limit are
  modifiable via config"
  (:require [qbits.flex.protocols :as p]))

(def defaults
  {:initial-limit 20
   :dec-by 0.98
   :min-limit 20
   :max-limit 200
   :inc-limit inc
   ;; 5s in ns
   :timeout (* 5 1e9)})

(defn- within-range
  [x min-limit max-limit]
  (-> x
      (min max-limit)
      (max min-limit)))

(defn make
  ([] (make {}))
  ([opts]
   (let [{:as _opts
          :keys [initial-limit inc-limit dec-by
                 timeout max-limit min-limit]}
         (merge defaults opts)
         limit (atom initial-limit)]
     (reify
       clojure.lang.IDeref
       (deref [_] @limit)

       p/Limit
       (-state [_] @limit)

       (-watch-limit! [_ k f]
         (add-watch limit
                    k
                    (fn [_k _r old new]
                      (when (not= old new)
                        (f new)))))

       (-update! [_ rtt-avgs rtt in-flight dropped?]
         (let [[old-rtt new-rtt] rtt-avgs]
           (swap! limit
                  (fn [limit-val]
                    (-> (cond
                          ;; faulty or timeout or new avg rtt too large
                          ;; -> decrease limit
                          (or dropped?
                              (> new-rtt timeout)
                              (> new-rtt old-rtt))
                          (long (* dec-by limit-val))

                          ;; happy path, we can increase limits
                          :else
                          (inc-limit limit-val))
                        (within-range min-limit max-limit))))))))))
