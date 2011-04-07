;; Copyright (C) 2011, Jozef Wagner. All rights reserved. 

(ns piaget.comparison
  "Comparison types"
  (:refer-clojure :exclude [compare]))

;;;; Public API

(defrecord LessThan [contents comp-fn])

(defrecord MoreThan [contents comp-fn])

(defrecord LessThanOrEqual [contents comp-fn])

(defrecord MoreThanOrEqual [contents comp-fn])

(defn compare [compare-type value]
  ((:comp-fn compare-type) (:contents compare-type) value))

;;;; Examples

(comment


)
