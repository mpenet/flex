(ns playground
  (:require [ring.adapter.jetty :as j]
            [exoscale.ex :as ex]
            [qbits.flex.limit.aimd :as limit]
            [qbits.flex.limiter.simple :as limiter]
            [qbits.flex.interceptor :as ix]
            [qbits.flex.middleware]
            [exoscale.interceptor]))

(def ok-response {:status 200
                  :body "Ok"})

(def rejected-response {:status 420
                        :body "Enhance your calm"})

(def limit (qbits.flex.limit.aimd/make
            #:qbits.flex.limit.aimd{:initial-limit 20
                                    :max-limit 30
                                    :min-limit 3}))

(def limiter (limiter/make {:limit limit}))

(def ix (ix/interceptor {:limiter limiter}))

(defn interceptor-handler [request]
  (exoscale.interceptor/execute
   {:request request}
   [{:error (fn [_ctx err]
              (if (instance? clojure.lang.ExceptionInfo err)
                (assoc rejected-response
                       :body (format "Enhance your calm - %s"
                                     (pr-str @limiter)))
                {:status 500
                 :body "boom"}))

     :leave :response}

    #'ix
    {:enter (fn [{:as ctx ::ix/keys [current-in-flight]}]
              (Thread/sleep (rand-int 3000))
              (assoc ctx
                     :response
                     {:status 200
                      :body (str @limiter)}))}]))


(defn server+interceptor
  []
  (j/run-jetty #'interceptor-handler
               {:port 8080
                :join? false}))

(defn server+middleware []
  (j/run-jetty (qbits.flex.middleware/with-limiter
                 (fn [_]
                   (Thread/sleep (rand-int 5000))
                   ok-response)
                 {:limiter limiter})
               {:port 8080
                :join? false}))

;; (declare server)
;; (try (.stop server) (catch Exception _ :boom))
;; (def server (server+interceptor))
