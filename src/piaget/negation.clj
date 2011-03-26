;; Copyright (C) 2011, Jozef Wagner. All rights reserved. 

(ns piaget.negation
  "Negation type")

;;;; Public API

(defrecord Negation [contents])

(defn neg
  "Negates a given value.
  Negation of a negation returns original contents."
  [value]
  (if (= Negation (type value))
    (:contents value)
    (Negation. value)))

;;;; Examples

(comment

  (neg 1)

  (neg nil)

  (neg (neg 1))

)
