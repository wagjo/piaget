;; Copyright (C) 2011, Jozef Wagner. All rights reserved. 

(ns piaget.pattern
  (:require [piaget.connector.hpa]
            [piaget.connector]
            [piaget.event]
            [piaget.alias])
  (:use [clojure.contrib.logging :only (info debug warn error)])
  (:import [piaget.connector.hpa Hpa]
           [piaget.event Event]))

;;; Pattern Fragment Entry

;; Pattern Fragment Entry is a key-value pair

;; Pattern Fragment Entry key must be of Keyword or String type

;; Pattern Fragment Entry value:
;; - String, Integer - match given value
;; - Keyword - define variable
;; - modified value - see below

;; Pattern Fragment Entry Value modifiers

(defrecord EntryValueNegation. [value])

(defn neg
  "Negates a given value"
  [value]
  (EntryValueNegation. value))

;;; Pattern Fragment
;; Pattern fragment can be a clojure map or instance of Fragment
;; record.
;; If fragment is a clojure map, search will try to match all its
;; entries (logical and on the entries)
;; TODO: define possible contents in the fragment record. Idea is to
;; support clojure sets for or and vectors for and to allow more
;; elaborate fragments


(defprotocol FragmentProtocol
  (get-fragment [this]))

(defrecord Fragment [contents]
  FragmentProtocol
  (get-fragment [this] contents))

(extend-protocol FragmentProtocol
  clojure.lang.IPersistentMap
  (get-fragment [this] this))

(defn fragment?
  "Returns true if e is pattern fragment"
  [e]
  (or (map? e) (= Fragment (type e))))

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
  (ConstrainedRelationship. :near relationship))

;;; Conditions

;; Condition is a map of given facts which have to be valid all the time

;;; Searching

(defn search
  "Searches for pattern occurences in the given dataset.
  Conditions contain additional starting parameters."
  [dataset pattern conditions])

;;;; Examples

(comment

  (type {:a :f})

  (def a (PatternElement. 1))

  (= PatternElement (type a))
  
)
