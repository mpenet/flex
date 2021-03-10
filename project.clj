(defproject cc.qbits/flex "1.0.0-alpha1"
  :url "https://github.com/mpenet/flex"

  :dependencies [[org.clojure/clojure "1.10.2"]
                 [exoscale/ex "0.3.17"]
                 [exoscale/interceptor "0.1.9"]]

  ;; :jvm-opts ["-XX:-OmitStackTraceInFastThrow"]

  :profiles {:dev {:dependencies [[ring/ring "1.9.1"]]}}

  :global-vars {*warn-on-reflection* true})
