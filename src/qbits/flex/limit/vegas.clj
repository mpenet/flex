(ns qbits.flex.limit.vegas
  "Limiter based on TCP Vegas where the limit increases by alpha if the queue_use
  is small ({@literal <} alpha) and decreases by alpha if the queue_use is large
  ({@literal >} beta).

  Queue size is calculated using the formula,
  queue_use = limit − BWE×RTTnoLoad = limit × (1 − RTTnoLoad/RTTactual)

  For traditional TCP Vegas alpha is typically 2-3 and beta is
  typically 4-6.  To allow for better growth and stability at higher
  limits we set alpha=Max(3, 10% of the current limit) and beta=Max(6,
  20% of the current limit)"
  (:require [qbits.flex.protocols :as p]
            [qbits.flex.limit.impl :as impl])
  (:import (java.util.concurrent ThreadLocalRandom)))

(defn- log10
  [i]
  (int (Math/max (int 1)
                 (int (Math/log10 i)))))

(defn alpha
  [limit]
  (* 3 (impl/log10 (int limit))))

(defn beta
  [limit]
  (* 6 (impl/log10 (int limit))))

(defn threshold
  [limit]
  (* 6 (impl/log10 (int limit))))

(defn inc-limit [limit]
  (+ (double limit)
     (impl/log10 (int limit))))

(defn dec-limit [limit]
  (- (double limit)
     (impl/log10 (int limit))))

(defn probe?
  [limit probe-count jitter probe-multiplier]
  (<= (* probe-count jitter probe-multiplier)
      limit))

(defn- smoothed-limit
  [new-limit max-limit smoothing limit]
  (int (+ (* limit (- 1 smoothing))
          (* (max 1
                  (min max-limit new-limit))
             smoothing))))

(defn- estimated-limit
  [limit rtt-no-load max-limit smoothing rtt in-flight dropped?]
  (cond
    ;; Treat any drop (i.e timeout) as needing to reduce the limit
    dropped?
    (smoothed-limit (dec-limit limit)
                    max-limit
                    smoothing
                    limit)

    ;; Prevent upward drift if not close to the limit
    (< (* 2 in-flight) limit)
    limit

    :else
    (let [beta-limit (beta (int limit))
          queue-size (int (Math/ceil (* limit
                                        (- (/ (double rtt-no-load)
                                              rtt)))))
          new-limit (cond
                      ;; Aggressive increase when no queuing
                      (<= queue-size threshold)
                      (+ beta-limit limit)

                      ;; Increase the limit if queue is still manageable
                      (< (alpha limit) queue-size)
                      (inc-limit limit)

                      ;; Detecting latency so decrease
                      (> queue-size beta-limit)
                      (dec-limit limit))]
      (if new-limit
        (smoothed-limit new-limit
                        max-limit
                        smoothing
                        limit)
        limit))))

(def defaults
  {::initial-limit 20
   ::max-limit 1000
   ::probe-multiplier 30
   ::smoothing 1.0})



(defn make
  [opts]
  (let [{:as _opts
         ::keys [initial-limit max-limit probe-multiplier
                 in-flight rtt smoothing]}
        (merge defaults opts)
        state (atom {:limit initial-limit
                     :probe-count 0
                     :probe-jitter (-> (ThreadLocalRandom/current)
                                       (.nextDouble 0.5 1))
                     :rtt-no-load 0})]
    (reify
      clojure.lang.IDeref
      (deref [_] (:limit @state))

      p/Limit
      (-state [_] @state)
      (-add-watch! [_ k f]
        (add-watch atm
                   k
                   (fn [_k _r
                        {old-limit :limit}
                        {new-limit :limit}]
                     (when (not= old-limit
                                 new-limit)
                       (f new-limit)))))

      (-update! [_ rtt in-flight dropped?]
        (-> (swap! state
                   #(fn [{:keys [limit rtt-no-load probe-count probe-jitter]}]
                      (let [probe-count (inc probe-count)]
                        (condp
                            (probe? limit
                                    probe-count
                                    probe-jitter
                                    probe-multiplier)
                            (assoc %
                                   :probe-jitter (-> (ThreadLocalRandom/current)
                                                     (.nextDouble 0.5 1))
                                   :probe-count 0
                                   :rtt-no-load rtt)

                          (or (zero? (:rtt-no-load %))
                              (> rtt (:rtt-no-load %)))
                          (assoc %
                                 :probe-count probe-count
                                 :rtt-no-load rtt)

                          :else
                          (assoc %
                                 :probe-count probe-count
                                 :limit (estimated-limit limit rtt-no-load
                                                         max-limit smoothing rtt
                                                         in-flight dropped?))))))
            :limit)))))
