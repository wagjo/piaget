;; Copyright (C) 2011, Jozef Wagner. All rights reserved. 

(ns piaget.pattern.fragment
  "Match Pattern Fragments"
  (:use [piaget.pattern.entry :only (match-entry alias-entry)]))

;;; Pattern Fragment

;; Pattern fragment can be a clojure map or instance of Fragment
;; record.
;; If fragment is a clojure map, search will try to match all its
;; entries (logical and on the entries)
;; TODO: define possible contents in the fragment record. Idea is to
;; support clojure sets for or and vectors for and to allow more
;; elaborate fragments

(defprotocol FragmentProtocol
  (match-fragment* [this event bindings])
  (alias-fragment [this aliases]))

(defrecord Fragment [contents]
  FragmentProtocol
  (match-fragment* [this event bindings]
                  nil                   ; TODO: implement
                  ))

(extend-protocol FragmentProtocol
  clojure.lang.IPersistentMap
  (match-fragment* [this event bindings*]
                   ;; first see if :start matches
                   (when (or (nil? (:start bindings*))
                             (neg? (compare (:start bindings*)
                                            (:start event))))
                     ;; match all Fragment entries
                     (loop [entries this
                            bindings #{bindings*}]
                       (if entries
                         (recur (next entries) ; match next entry
                                ;; must match current entry on all
                                ;; available bindings
                                (set (mapcat #(match-entry (first entries) event %)
                                             bindings)))
                         bindings))))
  (alias-fragment [this aliases]
                  ;; translate all values
                  (reduce conj {} (map #(alias-entry % aliases) this))))

;;;; Public API

(defn fragment?
  "Returns true if e is a Pattern Fragment."
  [e]
  (or (map? e) (= Fragment (type e))))

(defn match-fragment
  "Matches Pattern Fragment with a given event. Uses specified
   bindings. Returns set of possible bindings when matching is successfull,
   nil otherwise."
  [this event bindings]
  (match-fragment* this event bindings))
