(defproject cc.qbits/flex "1.0.0-alpha1-SNAPSHOT"
  :url "https://github.com/mpenet/flex"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.10.2"]
                 [exoscale/ex "0.3.17"]
                 [exoscale/interceptor "0.1.9"]]

  ;; :jvm-opts ["-XX:-OmitStackTraceInFastThrow"]

  :profiles {:dev {:dependencies [[ring/ring "1.9.1"]]}}

  :global-vars {*warn-on-reflection* true})
