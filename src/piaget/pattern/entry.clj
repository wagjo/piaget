;; Copyright (C) 2011, Jozef Wagner. All rights reserved. 

(ns piaget.pattern.entry
  "Match Pattern Fragment Entries"
  (:use [piaget.pattern.value :only (match-value alias-value)]))

;;; Pattern Fragment Entry

;; Pattern Fragment Entry is a key-value pair
;; - Its key must be of Keyword or String type
;; - Its value is described in piaget.pattern.value ns

(defn match-entry
  "Matches Pattern Fragment Entry with a given event. Uses specified
   bindings. Returns set of possible bindings when matching is successfull,
   nil otherwise."
  [[key value] event bindings]
  (match-value value (key event) bindings))

(defn alias-entry
  [[key value] aliases]
  [key (alias-value value aliases)])
