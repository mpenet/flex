(ns qbits.flex.limit.aimd
  "Implementation of AIMD Limit,
  https://en.wikipedia.org/wiki/Additive_increase/multiplicative_decrease

  By default it will increase by 1 on low avg rtt and backoff by 0.9
  on elevated/ing latency, both functions of current limit are
  modifiable via config"
  (:require [qbits.flex.protocols :as p]))

(def defaults
  {:initial-limit 20
   :min-limit 20
   :max-limit 200
   :limit-dec (fn [limit] (long (* 0.9 limit)))
   :limit-inc inc
   :inc-condition (fn [limit in-flight]
                    (>= (* 2 in-flight)
                        limit))

   ;; 5s in ns
   :timeout (* 5 1e9)})

(defn make
  ([] (make {}))
  ([opts]
   (let [{:as _opts
          :keys [initial-limit limit-dec limit-inc
                 timeout max-limit min-limit
                 inc-condition]}
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
                  (->> (cond
                         (or dropped? (> rtt timeout))
                         (limit-dec limit-val)

                         (inc-condition limit-val in-flight)
                         (limit-inc limit-val)

                         :else limit-val)
                       (max min-limit)
                       (min max-limit)))))))))
