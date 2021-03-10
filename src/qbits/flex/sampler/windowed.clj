(ns qbits.flex.sampler.windowed
  "Averaging sampler based on naive ring buffer"
  (:require [qbits.flex.protocols :as p]))

(defn avg
  [xs]
  (if (seq xs)
    (long (/ (reduce + 0 xs)
             (count xs)))
    0))

(defn make
  ([] (make {}))
  ([{::keys [length]
     :or {length 100}}]
   (let [q (atom clojure.lang.PersistentQueue/EMPTY)]
     (reify p/Sampler
       (-sample! [_ rtt]
         (-> (swap! q
                    (fn [q-val]
                      (conj (cond-> q-val
                              (>= (count q-val) length)
                              pop)
                            rtt)))
             avg))
       clojure.lang.IDeref
       (deref [_]
         (avg @q))))))
