(ns s-exp.flex.limit.aimd
  "Implementation of AIMD Limit,
  https://en.wikipedia.org/wiki/Additive_increase/multiplicative_decrease

  By default it will increase by 1 on low avg rtt and backoff by 0.9
  on elevated/ing latency, both functions of current limit are
  modifiable via config"
  (:require [s-exp.flex.protocols :as p]))

(def defaults
  {:initial-limit 20
   ;; :round-fn (fn [rtt-avg] (*))
   :dec-by 0.90
   :min-limit 20
   :max-limit 200
   :inc-limit inc
   :timeout 5000})

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

       (-update! [_ rtt in-flight dropped?]
         (swap! limit
                (fn [limit-val]
                  (-> (if (or dropped? (> rtt timeout))
                        (long (* dec-by limit-val))
                        (inc-limit limit-val))
                      (within-range min-limit max-limit)))))))))
