(ns s-exp.flex.limit.fixed
  (:require [s-exp.flex.protocols :as p]))

(def defaults {:limit 20})

(defn make
  [opts]
  (let [{::keys [limit]} (merge defaults opts)]
    (reify
      clojure.lang.IDeref
      (deref [_] limit)
      p/Limit
      (-state [_] {:limit limit})
      (-watch-limit! [_ k f])
      (-update! [_ rtt in-flight dropped?]
        limit))))
