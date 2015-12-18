(ns log-demo.logging
  (:require [taoensso.timbre :refer [info warn error errorf merge-config!] :as timbre]
            [dire.core :refer [with-wrap-hook! with-pre-hook! with-post-hook!]]
            [clojure.string :refer [upper-case join]]
            [cheshire.core :refer [generate-string]]
            [clj-time.format :as f]))

(defn- add-around-hook
  "Creates hooks around the target function."
  [task-var pre-fn post-fn]
  (with-pre-hook! task-var pre-fn)
  (with-wrap-hook! task-var post-fn))

(defn- get-tracking-id
  "Returns the tracking-id from the kafka message"
  [msg]
  (get-in msg [:value :tracking-id]))

(def ^:private ^:dynamic tid nil)

(defmacro set-tid
  "Sets tid (transaction id) for current logs,
   this id is later used in search to track a logical transaction (in graylog2 or kibana) of a series of logs."
  [tid* & body]
  `(binding [tid ~tid*] ~@body))

(comment
  (set-tid "my-tid" (prn "MAIN THREAD" tid)))

(defn- get-tid []
  "Gets current tid"
  tid)

(defn- append-tid [m]
  (if tid (assoc m :tid tid) m))

(comment
  (format-stacktrace (Exception. "") {}))

(defn- format-stacktrace [err opts]
  (join "\n" (map str (.getStackTrace err))))

(defn- append-stacktrace* [{:keys [?err_ opts]} m]
  "Returns log event with stacktrace based on the options opts"
  (if-not (:no-stacktrace? opts)
    (when-let [err (force ?err_)]
      (assoc m :stacktrace (str (format-stacktrace err opts))))))

(defn- append-stacktrace [data m]
  (if-let [out-log-with-trace (append-stacktrace* data m)]
    out-log-with-trace
    m))

(defn- format-log-event
  [{:keys [level ?err_ vargs_ msg_ ?ns-str hostname_ timestamp_ ?line]}]
  {:timestamp @timestamp_
   :host @hostname_
   :level (upper-case level)
   :message @msg_
   :namespace ?ns-str})

(defn- output-fn
  ([data] (output-fn nil data))
  ([opts data]
   (->> data format-log-event (append-stacktrace data) append-tid generate-string)))

(def iso-timestamp-opts
  "Controls (:timestamp_ data) to return ISO 8601 formatted time"
  {:pattern "yyyy-MM-dd'T'HH:mm:ss.SSSXXX" ;1997-07-16T19:20:30.45
   :locale (java.util.Locale. "en")
   :timezone (java.util.TimeZone/getDefault)})

(defn exception-handler [throwable ^Thread thread]
  (errorf throwable "Uncaught exception on thread: %s"
          (.getName thread)))

(defn init-logging []
  (timbre/handle-uncaught-jvm-exceptions!)
  (merge-config! {:output-fn output-fn
                  :timestamp-opts iso-timestamp-opts}))

(defn- log
  [msg opts level-macro]
  (level-macro (apply format (concat [msg] (vec opts)))))

(defn log-info
  "Logs a message with info level.
  Accepts a message and optional formatting values. "
  [msg & opts]
  (log msg opts #(info %)))

(defn log-error
  "Logs a message with error level.
  Accepts a message and optional formatting values. "
  [msg & opts]
  (log msg opts #(error %)))

(defn log-warn
  "Logs a message with warn level.
  Accepts a message and optional formatting values. "
  [msg & opts]
  (log msg opts #(warn %)))

(defn register-transaction-event-hook
  "This creates a logging hook for the register-transaction-event"
  [task-var]
  (let [get-tracking-id #(get-in % [:value :tracking-id])]
    (add-around-hook task-var
                     (fn [msg]
                       (info (str "Starting processing payment event from topic: " (:topic msg)
                                  " For Payment tracking-id: " (get-tracking-id msg))))
                     (fn [result [msg]]
                       (info (str "Finished processing payment event for payment tracking-id: " (get-tracking-id msg)))))))

(defn register-bank-transaction-event-hook
  "This creates a logging hook for the process-bank-transaction-message"
  [task-var]
  (let [get-tracking-id #(get-in % [:value :tracking-id])]
    (add-around-hook task-var
                     (fn [msg]
                       (info (str "Starting processing bank transaction event from topic: " (:topic msg)
                                  " With tracking-id: " (get-tracking-id msg))))
                     (fn [result [msg]]
                       (info (str "Finished processing bank transaction event with tracking-id: " (get-tracking-id msg)))))))

(defn register-release-transactions-hook
  [task-var]
  (add-around-hook task-var
                   (fn []
                     (info "Starting to release buffered transactions."))
                   (fn [result []]
                     (info "Finished releasing buffered transactions."))))

(defn register-check-pending-reconciliations-hook
  [task-var]
  (add-around-hook task-var
                   (fn []
                     (info "Starting to check pending reconciliations."))
                   (fn [result []]
                     (info "Finished checking pending reconciliations."))))

(defn register-retry-failed-reconciliations-hook
  [task-var]
  (add-around-hook task-var
                   (fn []
                     (info "Starting to retry failed reconciliations."))
                   (fn [result []]
                     (info "Finished retrying failed reconciliations."))))
