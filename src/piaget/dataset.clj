(ns piaget.dataset
  (:require [piaget.connector.hpa]
            [piaget.connector]
            [piaget.event]            
            [piaget.alias])
  (:import [piaget.connector.hpa Hpa]
           [piaget.event Event]))

(defrecord Dataset [data aliases filter connector])

(defn- alias-event [e]
  "Alias values in the event so it takes less memory."  
  (let [new-event (Event. (:id e) (:start e))
        processed-event (dissoc e :id :start)
        should-translate? #(not (#{:end} %))
        do-alias (fn [[k v]]
                   [k (if (should-translate? k) (piaget.alias/alias v) v)])]
    (reduce conj new-event (map do-alias processed-event))))

(defn transform-event
  "Should return a translated event and an updated aliases"
  [e aliases]
  (let [new-event (Event. (:id e) (:start e))
        processed-event (dissoc e :id :start)
        should-translate? #(not (#{:end} %))
        value-seq (remove nil? (map (fn [[k v]] (if (should-translate? k) v nil)) processed-event))
        new-aliases (reduce piaget.alias/add-new aliases value-seq)
        do-alias (fn [[k v]]
                   [k (if (should-translate? k) (piaget.alias/alias new-aliases v) v)])]
    [(reduce conj new-event (map do-alias processed-event)) new-aliases]))

(defn create-dataset [connector filter]
  (let [data (piaget.connector/load-events connector filter)
        parsed-data (reduce (fn [[evts a] e] (let [result (transform-event e a)]
                                       [(cons (result 0) evts) (result 1)]))
                            [nil (piaget.alias/create-aliases)] data)]
    (Dataset. (parsed-data 0) (parsed-data 1) filter connector )))

(comment
  
  (def hpa-connector (Hpa. "http://localhost:8084/hpa-prod/api/query.jsp" "kplab"))

  (def sample-filter {:from 40000 :count 6 :object-belongs-to "http://www.kp-lab.org/system-model/TLO#Root_Space"})

  (def sample-filter {:from 40000 :count 6 :type #{"creation" "modification"}})

  (piaget.connector/load-events hpa-connector sample-filter)
  
  (time (def r (create-dataset hpa-connector sample-filter)))

  (count (:data r))

)
