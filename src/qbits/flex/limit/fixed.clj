(ns qbits.flex.limit.fixed
  (:require [qbits.flex.protocols :as p]))

(def defaults {:limit 20})

(defn make
  [{:as _opts ::keys [limit]}]
  (reify
    clojure.lang.IDeref
    (deref [_] limit)
    p/Limit
    (-state [_] {:limit limit})
    (-add-watch! [_ k f])
    (-update! [_ rtt in-flight dropped?]
      limit)))
