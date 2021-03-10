(ns qbits.flex.limit.impl)

(defn add-watch!
  [atm k f]
  (add-watch atm
             k
             (fn [_k _r
                  {old-limit :limit}
                  {new-limit :limit}]
               (when (not= old-limit
                           new-limit)
                 (f new-limit)))))
