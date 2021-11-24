(ns playground
  (:require [ring.adapter.jetty :as j]
            [exoscale.ex :as ex]
            [qbits.flex.limit.aimd :as limit]
            [qbits.flex :as f]
            [qbits.flex.interceptor :as ix]
            [qbits.flex.middleware]
            [exoscale.interceptor]))

;; (def tf (bound-fn* println))
;; (remove-tap tf)
;; (add-tap tf)

(defn ok-response [s]
  {:status 200
   :body (pr-str s)})

(defn rejected-response [s]
  {:status 420
   :body (format "Enhance your calm - %s" (ex-message s))})

(def limit (qbits.flex.limit.aimd/make
            {:initial-limit 30
             :max-limit 100
             :min-limit 1}))

(def limiter (f/limiter {:limit limit}))
(def ix (ix/interceptor {:limiter limiter}))

(defn interceptor-handler [request]
  (exoscale.interceptor/execute
   {:request request}
   [{:error (fn [_ctx err]
              (if (ex/type? err :qbits.flex/rejected)
                (rejected-response @limiter)
                {:status 500
                 :body (str "boom -" err)}))

     :leave :response}

    #'ix
    {:enter (fn [ctx]
              (Thread/sleep (rand-int 1000))
              (assoc ctx
                     :response
                     (ok-response @limiter)))}]))


(defn server+interceptor
  []
  (j/run-jetty #'interceptor-handler
               {:port 8080
                :join? false}))

(def resp-time (atom 500))

(defn server+middleware []
  (let [handler (qbits.flex.middleware/with-limiter
                  (fn [_]
                    (Thread/sleep
                     ;; 1000 ; simulate stable
                     (max 1 (swap! resp-time inc)) ; simulate slowing down
                     ;; (max 1 (swap! resp-time dec)) ; simulate faster resp times
                     )
                    (ok-response @limiter))
                  {:limiter limiter})]
    (j/run-jetty (fn [request]
                   (ex/try+ (handler request)
                            (catch :qbits.flex/rejected _
                              (rejected-response @limiter))))
                 {:port 8080
                  :join? false})))

(declare server)
(try (.stop server) (catch Exception _ :boom))
(def server (server+middleware))
