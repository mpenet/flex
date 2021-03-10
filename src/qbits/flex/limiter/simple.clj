(ns qbits.flex.limiter.simple
  (:require [qbits.flex.protocols :as p]
            [qbits.flex.recorder :as recorder]
            [qbits.flex.clock :as clock]
            [qbits.flex.sampler.windowed :as sampler]
            [exoscale.ex :as ex]))

(defn make
  [{:keys [clock sampler limit recorder]
    :or {recorder (recorder/make)
         clock (clock/make)
         sampler (sampler/make)}}]

  (reify p/Limiter
    (-acquire! [this]
      (let [current-limit @limit
            in-flight (p/-inc! recorder)
            time @clock]
        (when (<= in-flight current-limit)
          {:time time
           :in-flight in-flight
           :limit current-limit})))

    (-complete! [this start-time]
      (p/update! limit
                 (p/sample! sampler
                            (p/duration clock
                                        start-time))
                 (p/-dec! recorder)
                 false))

    (-ignore! [this]
      (p/dec! recorder))

    (-reject! [this]
      (p/dec! recorder)
      (p/update! limit
                 @sampler
                 @recorder
                 true))

    clojure.lang.IDeref
    (deref [_]
      {:in-flight @recorder
       :limit @limit
       :sample @sampler
       :time @clock})))
