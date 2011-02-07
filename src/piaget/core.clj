(ns piaget.core
  (:require [piaget.connector.hpa]
            [piaget.connector]
            [piaget.event]
            [piaget.alias])
  (:import [piaget.connector.hpa Hpa]
           [piaget.event Event]))

(defrecord Value [value probability negation])

;; dat protocol na resolvovanie
;; dat p a neg ako protocol, aby sli (p (neg :s) 0.4)

(defn p [value prob]
  value)

(defn neg [value]
  value)

(defn all [value & values]
  value) ;; pouziva sa pri keywordoch

;;(defmacro defpattern def)

(def p1
  {:user (p :s 0.2)
   :object (neg :x)
   :type :x
   :belongs-to #{1 2 3}
   :time [0 1]
   :x #{:y :z 5} ;; dat takto vztahy medzi keywordami???
   })

;; TODO: asi dat vztahy medzi keywordami samostatne

(def conditions
  {:x (neg :z)})

(comment
  ;; relation examples
  ;; nedavat tu map, tu uz bude konkretny pattern
  [p1 p2 p3]
  [p0 #{p1 p2} p3]
  )

(defn match [data pattern conditions]
  nil)

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

  )
