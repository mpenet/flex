(ns qbits.flex.limit.gradient2
  (:require [qbits.flex.protocols :as p]))

(defn factor
  [n]
  (/ 2.0 (int (inc n))))

(defn measurement-add
  [state-val sample]
  (let [{:keys [value sum count window warmup-window]} (:exp-avg-measurement state-val)]
    (if (< count warmup-window)
      (let [count (inc count)
            sum (+ sum (double sample))]
        (update state-val
                :exp-avg-measurement
                assoc
                :count count
                :sum (/ sum count)
                :value value))
      (let [factor' (factor window)]
        (update state-val
                :exp-avg-measurement
                assoc
                :value (+ (* value (- 1 factor'))
                          (* (double sample) factor')))))))

(def defaults {:initial-limit 20
               :limit 20
               :min-limit 20
               :max-concurrency 200
               :smoothing 0.2
               :long-window 600
               :rtt-tolerance 1.5
               :queue-size 4
               :last-rtt 0
               :exp-avg-measurement
               {:value 0.0
                :sum 0.0
                :count 0
                :window 10
                :warmup-window 600}})

(defn make
  ([] (make {}))
  ([opts]
   (let [{:as _opts
          :keys [initial-limit rtt-tolerance queue-size smoothing
                 min-limit max-limit exp-avg-measurement]} (merge defaults opts)
         state (atom {:limit initial-limit
                      :exp-avg-measurement exp-avg-measurement})]
     (reify
       clojure.lang.IDeref
       (deref [_] (:limit @state))

       p/Limit
       (-state [_] @state)

       (-watch-limit! [_ k f]
         (add-watch state
                    k
                    (fn [_k _r old new]
                      (when (not= (:limit old) (:limit new))
                        (f (:limit new))))))

       (-update! [_ rtt start-time in-flight dropped?]
         (swap! state
                (fn [{:as state-val :keys [limit]}]
                  (let [state-val (measurement-add state-val rtt)
                        short-rtt (double rtt)
                        long-rtt (-> state-val :exp-avg-measurement :value)
                        state-val (cond-> state-val
                                    ;; If the long RTT is substantially larger than
                                    ;; the short RTT then reduce the
                                    ;; long RTT measurement.
                                    ;; This can happen when latency returns to normal
                                    ;; after a prolonged prior of
                                    ;; excessive load.
                                    ;; Reducing the long RTT without waiting for
                                    ;; the exponential smoothing helps
                                    ;; bring the system back to steady
                                    ;; state.
                                    (> (/ long-rtt short-rtt) 2)
                                    (update :exp-avg-measurement update :value #(* 0.95 %)))]
                    ;; Don't grow the limit if we are app limited
                    ;; FIXME check about the /2
                    (if (> in-flight (/ limit 2))
                      state-val
                      ;; Rtt could be higher than rtt_noload because of
                      ;; smoothing rtt noload updates so set to 1.0 to
                      ;; indicate no queuing.  Otherwise calculate the
                      ;; slope and don't allow it to be reduced by more
                      ;; than half to avoid aggressive load-shedding due
                      ;; to outliers.
                      (let [gradient (max 0.5 (min 0.1 (* rtt-tolerance (/ long-rtt short-rtt))))
                            new-limit (+ (* limit gradient) queue-size)
                            new-limit (+ (* limit (- 1 smoothing))
                                         (* new-limit smoothing))
                            new-limit (max min-limit (min max-limit new-limit))]
                        (assoc state-val :limit (int new-limit))))))))))))
