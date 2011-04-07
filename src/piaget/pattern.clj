;; Copyright (C) 2011, Jozef Wagner. All rights reserved. 

(ns piaget.pattern
  "Performs a pattern search on a given dataset"
  (:require [piaget.connector.hpa]
            [piaget.connector]
            [piaget.event]
            [piaget.alias]
            [piaget.dataset])
  (:use [clojure.contrib.logging :only (info debug warn error)]
        [piaget.pattern.fragment :only (match-fragment alias-fragment)]
        [piaget.negation :only (neg)])
  (:import [piaget.connector.hpa Hpa]
           [piaget.event Event]))

(defn- extract-result
  [bindings]
  (:event bindings)
  #_(:id (:event bindings)))

;;; Relationships

;; Possible relationships:
;; - one pattern fragment - find matching element
;; - sequence of relationships - found results must follow one after
;;   another in terms of time
;; - set of relationship - found results are independent of each other
;;   in terms of time

(defprotocol PatternProtocol
  (match-pattern* [this dataset bindings])
  (alias-pattern [this aliases]))

(defrecord ConstrainedRelationship [constrain relationship])

(extend-protocol PatternProtocol
  ;; nil
  nil
  (match-pattern* [this events bindings]
                  nil)
  ;; one Fragment
  clojure.lang.IPersistentMap
  (match-pattern* [this events bindings]
                  (set (mapcat
                        #(match-fragment this %
                                         (assoc bindings :event %))
                        events)))
  (alias-pattern [this aliases]
                 (alias-fragment this aliases))
  ;; seqable
  clojure.lang.ISeq
  (match-pattern* [this events bindings]
                  ;; match first fragment
                  (let [result (match-pattern* (first this) events bindings)
                        next-bindings (map #(assoc % :start (:start (:event %))) result)
                        next-fn (fn [b]
                                  (let [next-result (match-pattern* (next this) events b)]
                                    (if next-result
                                      [(extract-result b) next-result]
                                      (extract-result b))))]
                    (when-not (empty? next-bindings)
                      (map next-fn next-bindings))))
  (alias-pattern [this aliases]
                 (map #(alias-fragment % aliases) this))
  ;; vector of Fragments
  clojure.lang.IPersistentVector
  (match-pattern* [this events bindings]
                  (match-pattern* (seq this) events bindings))
  (alias-pattern [this aliases]
                 (vec (map #(alias-fragment % aliases) this)))
  )

;; Relationship modifiers

(defn near
  "Limits results to be close to each other"
  [& relationships]
  (ConstrainedRelationship. :near relationships))

;;;; Public API

(defn search
  "Searches for pattern occurences in the given dataset.
  Conditions contain starting variable bindings,
  its keys must be Keywords which represent variables."
  ([dataset pattern] (search dataset pattern {}))
  ([dataset pattern conditions]
     (let [events (:data dataset)
           aliased-pattern (alias-pattern pattern (:aliases dataset))]
       (match-pattern* aliased-pattern events conditions))))

;;;; Examples

(comment

  (type {:a :f})

  (def a (PatternElement. 1))

  (= PatternElement (type a))



  (def hpa-connector (Hpa. "http://localhost:8084/hpa-prod/api/query.jsp" "kplab"))

  (def sample-filter {:count 20 :from 2000})

  (time (def d (piaget.connector/load-events hpa-connector sample-filter)))

  (time (def r (piaget.dataset/create-dataset hpa-connector sample-filter)))

  (def pe1
    {:type "creation"})

  (def pe2
       {:type (neg "modification")})

  (def pe3
       {:type "opening"})

  ((:alias->val (:aliases r)) 1)

  (map :type (search r [pe2]))

  (count (:data r))

  (:val->alias (:aliases r))

  (take 1 r)

  (alias-pattern {:x "creation"} (:aliases r))
  
  
)
