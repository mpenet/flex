(ns qbits.flex.ex
  (:require [exoscale.ex :as ex]))

(defn ex-rejected
  [data]
  (ex/ex-info "Request rejected"
              [:qbits.flex/rejected [:exoscale.ex/busy]]
              data))

(defn ex-rejected!
  [data]
  (throw (ex-rejected data)))
