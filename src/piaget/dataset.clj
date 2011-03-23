;; Copyright (C) 2011, Jozef Wagner. All rights reserved. 

(ns piaget.dataset
  "Create dataset for analysis"
  (:require [piaget.connector.hpa]
            [piaget.connector]
            [piaget.event]            
            [piaget.alias])
  (:import [piaget.connector.hpa Hpa]
           [piaget.event Event]))

;;;; Implementation Details

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

;;;; Public API

(defn create-dataset [connector filter]
  "Creates a dataset based on specified filter and connection"
  (let [data (piaget.connector/load-events connector filter)
        parse-fn (fn [[evts a] e] (let [result (transform-event e a)]
                                   [(cons (result 0) evts) (result 1)]))
        [parsed-data aliases] (reduce parse-fn
                                      [nil (piaget.alias/create-aliases)]
                                      data)]
    (Dataset. parsed-data aliases filter connector)))

;;;; Examples

(comment
  
  (def hpa-connector (Hpa. "http://localhost:8084/hpa-prod/api/query.jsp" "kplab"))

  (def sample-filter {:count 10000
                      :belongs-to "http://www.kp-lab.org/system-model/TLO#CollectiveSpace:2008-10-28T08:33:01.498"})

  (def sample-filter {:count 20 :type #{"creation" "modification"}})

  (def sample-filter {:count 2000 :type "creation"})

  (def sample-filter {:count 20 :start ["2010-06" "2011"]})

  (def sample-filter {:count 6 :id 3})

  (time (def r (create-dataset hpa-connector sample-filter)))

  (count (:data r))

)
