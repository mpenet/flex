(ns qbits.flex.clock
  (:require [qbits.flex.protocols :as p]))

(defn make
  ([] (make {}))
  ([_opts]
   (reify p/Clock
     (-duration [_ t] (- (System/nanoTime) t))
     clojure.lang.IDeref
     (deref [_] (System/nanoTime)))))
