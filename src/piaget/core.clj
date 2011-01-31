(ns piaget.core
  (:require [piaget.connector.hpa]
            [piaget.connector]
            [piaget.event]
            [piaget.alias])
  (:import [piaget.connector.hpa Hpa]
           [piaget.event Event]))


(def kplab-connector (Hpa. "http://localhost:8084/hpa-prod/api/query.jsp" "kplab"))

(defn- alias-event [e]
  "Alias values in the event so it takes less memory."  
  (let [new-event (Event. (:id e) (:start e))
        processed-event (dissoc e :id :start)
        should-translate? #(not (#{:end} %))
        do-alias (fn [[k v]]
                   [k (if (should-translate? k) (piaget.alias/alias v) v)])]
    (reduce conj new-event (map do-alias processed-event))))

(defn- alias-events [events]
  "Alias values in the event so it takes less memory."
  (map alias-event events))

(defn a [x]
  (do (doall (alias-events (piaget.connector/load-events kplab-connector {:from 5 :count x})))
      #_(:last-alias @piaget.alias/default-aliases)))

(defn b [x]
  (do (dorun (piaget.connector/load-events kplab-connector {:from 5 :count x}))
      nil))



(comment

  piaget.connector.hpa/tr-map
  
  (def kplab-connector (hpa/Hpa. "http://localhost:8084/hpa-prod/api/query.jsp" "kplab"))

(a 3)
  
  (piaget.connector/resource-name kplab-connector
                                  ["http://www.kp-lab.org/system-model/TLO#Task_1.4"
                                   "http://www.kp-lab.org/system-model/TLO#Student_Paul"])

  (piaget.connector/resource-name kplab-connector "http://www.kp-lab.org/system-model/TLO#Task_1.4")

  (def h (http/http-agent "http://kplab.tuke.sk:8080/hpa-prod/api/query.jsp" :method "POST" :body "token=kplab&fn=get-activity&args=30000"))
  
  )
