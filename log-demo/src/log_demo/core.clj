(ns log-demo.core
  (:require [log-demo.logging :refer :all]
            [taoensso.timbre :refer [info]])
  (:gen-class))

(defn -main
  [& args]
  (init-logging)

  (info "Starting service...")

  (info (Exception. "Something went wrong."))

  (throw (Exception. "Killing app"))
  )
