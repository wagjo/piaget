;; Copyright (C) 2011, Jozef Wagner. All rights reserved. 

(ns piaget.pattern
  "Performs a pattern search on a given dataset"
  (:require [piaget.connector.hpa]
            [piaget.connector]
            [piaget.connector.wiki]
            [piaget.event]
            [piaget.alias]
            [piaget.dataset])
  (:use [clojure.contrib.logging :only (info debug warn error)]
        [piaget.pattern.fragment :only (match-fragment alias-fragment)]
        [piaget.negation :only (neg)])
  (:import [piaget.connector.hpa Hpa]
           [piaget.connector.wiki Wiki]
           [piaget.event Event]))

(defn- extract-result
  [bindings]
  bindings
  #_(:event bindings)
  #_(:id (:event bindings)))

(def *fail* (atom #{}))

(def *saved* (atom 0))

(defn clear-failed! []
  (info "fails cleared")
  (reset! *saved* 0)
  (reset! *fail* #{}))

(defn is-failed? [b]
  (and (contains? @*fail* b)
       (do (swap! *saved* inc) true)))

(defn failed! [b]
  #_(error b)
  (swap! *fail* conj b))

(def *count* (atom 0))

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

(defn special-event-seq [events]
  (lazy-seq
   (when events
     (cons [(first events) (next events)] (special-event-seq (next events))))))

(extend-protocol PatternProtocol
  ;; nil
  nil
  (match-pattern* [this events bindings]
                  nil)
  ;; one Fragment
  clojure.lang.IPersistentMap
  (match-pattern* [this events bindings]
                  ;; set was here, but we want something like lazy set
                  (let [bindings (assoc bindings :pattern (conj (:pattern bindings) this))
                        f (dissoc bindings :start :event :next-events)]
                    (if (is-failed? f)
                      (do (info (str "saved a bunch of time " f)) nil)
                      (let [res (mapcat
                                 (fn [es]
                                   (swap! *count* inc)
                                   (map #(assoc % :next-events (second es))
                                              (match-fragment this (first es)
                                                              (assoc bindings :event (first es)))))
                                 (special-event-seq events))]
                        (when (empty? res)
                          (failed! f))
                        res))))
  (alias-pattern [this aliases]
                 (alias-fragment this aliases))
  ;; seqable
  clojure.lang.ISeq
  (match-pattern* [this events bindings]
                  ;; match first fragment
                  (let [result (match-pattern* (first this) events bindings)
                        next-bindings (map #(assoc % :start (:start (:event %))) result)
                        next-fn (fn [b]
                                  (let [next-result (match-pattern* (next this) (:next-events b) b)]
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
     (clear-failed!)
     (reset! *count* 0)
     (let [events (:data dataset)
           aliased-pattern (alias-pattern pattern (:aliases dataset))]
       (match-pattern* aliased-pattern events conditions))))

;;;; Examples

(comment

  (type {:a :f})

  (def a (PatternElement. 1))

  (= PatternElement (type a))

  (def wiki-connector (Wiki. "reverts-sorted.clj" 1000))

  (def r (piaget.dataset/create-fake-dataset wiki-connector nil))  

  (first (filter #(and (vector? %)
                       (vector? (first (second %)))) s))

  (count @*fail*)

  (first @*fail*)

  @*saved*

  @*count*

  (first s)

  (time (count s))

  ;; Same admin reverted culprit on two different pages

  (def f1 {:actor :X :entity :Y :culprit :Z})

  (def f2 {:actor :X :entity (neg :Y) :culprit :Z})

  (def s (search r [f1 f2]))

  ;; Two admins reverted same culprit on same document

  (def f3 {:actor :ADMIN1 :culprit :C :entity :E})

  (def f4 {:actor :ADMIN2 :culprit :C :entity :E})

  (def s (search r [f3 f4] {:ADMIN1 (neg :ADMIN2)}))

  ;; Two admins reverted same culprit on different document

  (def f5 {:actor :ADMIN1 :culprit :C :entity :E})

  (def f6 {:actor :ADMIN2 :culprit :C :entity (neg :E)})

  (def s (search r [f5 f6] {:ADMIN1 (neg :ADMIN2)}))

  ;; Three different admins reverted same culprit every time on
  ;; different entity

  (def f7 {:actor :ADMIN1 :culprit :C :entity :E1})

  (def f8 {:actor :ADMIN2 :culprit :C :entity :E2})

  (def f9 {:actor :ADMIN3 :culprit :C :entity :E3})

  (def s (search r [f7 f8 f9] {:ADMIN2 (neg :ADMIN1)
                               :ADMIN3 [(neg :ADMIN2)
                                        (neg :ADMIN1)]
                               :E2 (neg :E1)
                               :E3 [(neg :E2)
                                    (neg :E1)]}))
  
  (def s (search r [f7 f8 f9]))
  
  (first (filter #(and (vector? %)
                       (vector? (first (second %)))) s))

  (first s)
  
  (def f5 {:actor :Z :culprit :C})  

  (def d (take 10 (piaget.connector/load-events wiki-connector nil)))



  (take 1 (:data r))

  (first d)

  ;; TODO: negation is bugged

  (first s)

  (defn a [x]
    (> (count x) 2))

  (first s)

  (second (filter vector? s))
  (first (filter #(and (vector? %)
                       (some a (next %))) s))
  
  (first (filter #(do (info %) (vector? %)) (search r [f3 f4] {:X (neg :Y)
                                            :Y (neg :Z)
                                            :Z (neg :X)})))



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
