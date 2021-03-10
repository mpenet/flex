(ns playground
  (:require [ring.adapter.jetty :as j]
            [exoscale.ex :as ex]
            [qbits.flex.limit.aimd :as limit]
            [qbits.flex.sampler.windowed :as sampler]
            [qbits.flex.clock.sys :as clock]
            [qbits.flex.recorder.memory :as recorder]
            [qbits.flex.limiter.simple :as limiter]
            [qbits.flex.interceptor :as ix]
            [qbits.flex.middleware]
            [exoscale.interceptor]))

(def ok-response {:status 200
                  :body "Ok"})

(def rejected-response {:status 420
                        :body "Enhance your calm"})

(def limit (qbits.flex.limit.aimd/make
            #:qbits.flex.limit.aimd{:initial-limit 2
                                    :max-limit 5
                                    :min-limit 1}))
(def limiter (limiter/make))
(def sampler (sampler/make {}))
(def recorder (recorder/make {}))
(def clock (clock/make {}))

(def ix (ix/interceptor {:limiter limiter
                         :recorder recorder
                         :sampler sampler
                         :limit limit
                         :clock clock}))

(defn handler [request]
  (exoscale.interceptor/execute
   {:request request}
   [{:error (fn [_ctx err]
              (if (ex/type? err :qbits.flex/rejected)
                (assoc rejected-response
                       :body (format "Enhance your calm - %s"
                                     (pr-str {:limit @limit
                                              :sample (format "%sms" (some-> @sampler (/ 1e6)))})))
                {:status 500
                 :body "boom"}))
     :leave :response}

    #'ix
    {:enter (fn [{:as ctx ::ix/keys [current-in-flight]}]
              (Thread/sleep (rand-int 1500))
              (assoc ctx
                     :response
                     {:status 200
                      :body (str {:limit @limit
                                  :in-flight current-in-flight
                                  :sampler (when @sampler
                                             (format "%sms"
                                                     (some-> @sampler
                                                             (/ 1e6))))})}))}]))

;; (.stop server)
(def server
  (j/run-jetty #'handler
               {:port 8080
                :join? false}))

;; (def server
;;   (j/run-jetty (qbits.flex.middleware/with-limiter (fn [_]
;;                                                         (Thread/sleep (rand-int 1000))
;;                                                         ok-response)
;;                  limiter
;;                  sampler)
;;                {:port 8080
;;                 :join? false}))
