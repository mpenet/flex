(ns playground
  (:require [ring.adapter.jetty :as j]
            [exoscale.ex :as ex]
            [qbits.flex.limit.aimd :as limit]
            [qbits.flex :as f]
            [qbits.flex.interceptor :as ix]
            [qbits.flex.middleware]
            [exoscale.interceptor]))

(defn ok-response [s]
  {:status 200
   :body (pr-str s)})

(defn rejected-response [s]
  {:status 420
   :body (format "Enhance your calm - %s" (pr-str s))})

(def limit (qbits.flex.limit.aimd/make
            {:initial-limit 1
             :max-limit 3
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

(defn server+middleware []
  (j/run-jetty (qbits.flex.middleware/with-limiter
                 (fn [_]
                   (Thread/sleep (rand-int 5000))
                   (ex/try+
                     (ok-response @limiter)
                     (catch :qbits.flex/rejected e
                       (rejected-response @limiter))))
                 {:limiter limiter})
               {:port 8080
                :join? false}))

;; (declare server)
;; (try (.stop server) (catch Exception _ :boom))
;; (def server (server+interceptor))
