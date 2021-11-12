(ns qbits.flex.sampler.decaying
  "Records rtts with ttl value set at creation of the sampler. All but
  the last RTT recorded will expire over time. This should allow for
  fast recovery after a long pause and be more fair overall to future
  requests"
  (:require [qbits.flex.protocols :as p]))

(defn avg
  [xs]
  (if (seq xs)
    (long (/ (reduce (fn [acc [_ val]]
                       (+ acc val))
                     0
                     xs)
             (count xs)))
    0))

(defn make
  ([] (make {}))
  ([{::keys [length ttl decay-interval]
     :or {length 25
          ttl 100
          decay-interval 100}}]
   (let [q (atom clojure.lang.PersistentQueue/EMPTY)
         _decay-process (future
                          (while (not (Thread/interrupted))
                            (swap! q
                                   (fn [q-val]
                                     (let [now (System/currentTimeMillis)]
                                       (into (clojure.lang.PersistentQueue/EMPTY)
                                             (remove (fn [[ts _val]]
                                                       (> now (+ ts ttl))))
                                             q-val))))
                            (Thread/sleep decay-interval)))]
     (reify p/Sampler
       (-sample! [_ rtt]
         (-> (swap! q
                    (fn [q-val]
                      (conj (cond-> q-val
                              (>= (count q-val) length)
                              pop)
                            [(System/currentTimeMillis) rtt])))
             avg))
       clojure.lang.IDeref
       (deref [_]
         (avg @q))))))

;; (def x (make {}))

;; (do (prn @x)
;;     (p/sample! x 5))
