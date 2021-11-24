(ns qbits.flex.sampler.windowed
  "Averaging sampler based on naive ring buffer"
  (:require [qbits.flex.protocols :as p]))

(defn avg
  [xs]
  (if (seq xs)
    (long (/ (reduce + 0 xs)
             (count xs)))
    0))

(defn ewma
  "Exponentially-weighted moving average"
  [xs a]
  (-> (reduce (fn [xs x]
                (conj xs
                      (+ (* a x)
                         (* (- 1 a)
                            (peek xs)))))
              [(first xs)]
              (rest xs))
      peek
      (or 0)))

(defn make
  ([] (make {}))
  ([{::keys [length averaging-f]
     :or {length 25
          averaging-f #(avg %)}}]
   (let [q (atom clojure.lang.PersistentQueue/EMPTY)]
     (reify p/Sampler
       (-sample! [_ rtt]
         (->> (swap-vals! q
                          (fn [q-val]
                            (conj (cond-> q-val
                                    (>= (count q-val) length)
                                    pop)
                                  rtt)))
              (map averaging-f)))
       clojure.lang.IDeref
       (deref [_]
         (averaging-f @q))))))
