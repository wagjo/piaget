;; Copyright (C) 2011, Jozef Wagner. All rights reserved. 

(ns piaget.connector.hpa
  (:use [clojure.contrib.str-utils :only [str-join]])
  (:require [piaget.connector]
            [clojure.contrib.http.agent :as http]
            [clojure.contrib.json :as json]
            [clojure.java.io :as io]))

;; TODO: do not use clojure.contrib.http.agent

;; Helper fns
(defn- url-encode [text]
  (java.net.URLEncoder/encode text "UTF-8"))

(defn- url-body [body]
  (str-join "&" (map #(str-join "=" (map (comp url-encode name) %)) body)))

(defn- http-post [url body]
  (let [handler #(let [w (java.io.StringWriter.)]
                   (try
                     (let [r (io/reader (http/stream %))]
                       #_(io/copy r w)
                       (json/read-json r))
                     (catch Exception e
                       {:handler-error true
                        :response (str w)})))]
    (http/result
     (http/http-agent url
                      :method "POST"
                      :body (url-body body)
                      :handler handler
                      :connect-timeout 60000
                      :read-timeout 60000))))

;; Translate keys according to event type specification

(def from-hpa
  {:act-time :start
   :actor-id :actor
   :entity-id :entity
   :object-belongs-to :belongs-to
   :artefact-refers-to :refers-to
   :used-tool :tool
   :description :desc
   :act_desc :desc})

(def to-hpa
  {:start :act-time
   :actor :actor-id
   :entity :entity-id
   :belongs-to :object-belongs-to
   :refers-to :artefact-refers-to
   :tool :used-tool
   :desc :act_desc})

(defn- tr-events [events]
  "Translate some keys in the event, based on event specification."
  (let [tr-event (fn [[k v]] [(or (from-hpa k) k) v])]
    (map #(reduce conj {} (map tr-event %)) events)))

(defn- prepare-filter [filter]
  (let [prepare-entry (fn [[k v]] [(or (to-hpa k) k) (if (set? v)
                                      (cons :should-be-set v)
                                      v)])]
    (reduce conj {} (map prepare-entry filter))))

(defn- handle-error [what]
  (if (map? what)
    (throw (RuntimeException. (str "error fetching events " (:response what))))
    what))

;; Hpa type, implementing Connector protocol

(deftype Hpa [url password]
  piaget.connector/Connector
  (load-events [this filter]
               (tr-events
                (handle-error
                 (http-post url {:token password
                                 :fn "load-events"
                                 :args (json/json-str (prepare-filter filter))
                                 :timeout "30000"}))))
  (resource-name [this ids]
    (http-post url {:token password
                    :fn "resource-name"
                    :args (json/json-str ids)
                    :timeout "30000"})))


(comment

  (def sample-filter {:count 20 :type #{"creation" "modification"}})

  (def kplab-connector (Hpa. "http://localhost:8084/hpa-prod/api/query.jsp" "kplab"))

  (map :type (piaget.connector/load-events kplab-connector sample-filter))
  
  (piaget.connector/resource-name kplab-connector
                                  ["http://www.kp-lab.org/system-model/TLO#Task_1.4"
                                   "http://www.kp-lab.org/system-model/TLO#Student_Paul"])

  (piaget.connector/resource-name kplab-connector "http://www.kp-lab.org/system-model/TLO#Task_1.4")

  )
