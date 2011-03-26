;; Copyright (C) 2011, Jozef Wagner. All rights reserved. 

(ns piaget.pattern
  "Performs a pattern search on a given dataset"
  (:require [piaget.connector.hpa]
            [piaget.connector]
            [piaget.event]
            [piaget.alias])
  (:use [clojure.contrib.logging :only (info debug warn error)]
        [piaget.pattern.fragment :only (match-fragment)]
        [piaget.negation :only (neg)])
  (:import [piaget.connector.hpa Hpa]
           [piaget.event Event]))

;;; Relationships

;; Possible relationships:
;; - one pattern fragment - find matching element
;; - sequence of relationships - found results must follow one after
;;   another in terms of time
;; - set of relationship - found results are independent of each other
;;   in terms of time

(defrecord ConstrainedRelationship [constrain relationship])

(defprotocol RelationshipProtocol
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
  [dataset pattern conditions])

;;;; Examples

(comment

  (type {:a :f})

  (def a (PatternElement. 1))

  (= PatternElement (type a))
  
)
