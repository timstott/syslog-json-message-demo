(defproject log-demo "0.1.0-SNAPSHOT"
  :description "Records all external payments that are processed through GoCardless."
  :url "http://github.com/FundingCircle/gocardless-handler"
  :license {:name "Not for distribution."
            :url "https://www.fundingcircle.com"}
  :dependencies [[cheshire "5.5.0"]                                              ;; JSON/JSONB encoding/decoding
                 [clj-time "0.11.0"]                                             ;; Joda Time wrapper
                 [com.taoensso/timbre "4.1.1"]                                   ;; logging and profiling
                 [org.clojure/clojure "1.7.0"]
                 [dire "0.5.3"]]                                                 ;; error handling and hooks

  :plugins [[s3-wagon-private "1.1.2"]
            [lein-cljfmt  "0.3.0"]
            [lein-midje "3.1.3"]]

  :main log-demo.core
  :min-lein-version "2.0.0"
  :aliases {}
  :profiles {:uberjar {:aot [log-demo.core]}}
  :repositories []
  :uberjar-name "log-demo.jar")

