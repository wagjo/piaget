;; Copyright (C) 2011, Jozef Wagner. All rights reserved. 

(ns piaget.pattern.value
  "Match Pattern Fragment Entry values"
  (:use [piaget.negation :only (neg)])
  (:import [piaget.negation Negation]))

;; Pattern Fragment Entry value can be:
;; - String, Integer - literal value
;; - Keyword - define variable
;; - Negation - negation of its contents
;; - Vector - AND - all its contents must match
;; - Set - OR - any of its contents can match
;; TODO: Range. MoreThan. LessThan.
;; TODO: Probability., maybe

;;;; Implementation details

(defn- merge-values
  "Merges two values to form one vector. Current value can be vector or not.
  Moreover, we do not want to have duplicate values in the result.
  Sets cannot be returned because we explicitly need a vector as a result."
  [new-value current-value]
  (if (= new-value current-value)
    ;; if values are same, returns only one of them
    current-value
    (if (vector? current-value)
      ;; adds new value to the current value,
      ;; but we do not want duplicate values in the vector
      (-> current-value
          set
          (conj new-value)
          vec)
      ;; makes a new vector if current-value is not vector
      [current-value new-value])))

(defprotocol MatchValueProtocol
  (match-value* [this literal bindings]))

(declare match-value)

(extend-protocol MatchValueProtocol
  ;; match if event does not contain a requested value
  nil
  (match-value* [this literal bindings]
               ;; literal can be an object or a negation of an object
               (if (= Negation (type literal))
                 (when-not (nil? (:contents literal))
                   #{bindings})
                 (when (nil? literal)
                   #{bindings})))
  ;; match string or integer (aliased) literal
  Object
  (match-value* [this literal bindings]
               ;; literal can be an object or a negation of an object
               (if (= Negation (type literal))
                 (when-not (= this (:contents literal))
                   #{bindings})
                 (when (= this literal)
                   #{bindings})))
  ;; match variable
  clojure.lang.Keyword
  (match-value* [variable literal bindings]
                (if-let [bound-value (variable bindings)]
                  ;; variable is already bound, match its binding
                  (set (map #(assoc % variable (merge-values literal bound-value))
                            (match-value bound-value literal (dissoc bindings variable))))
                  ;; variable not bound, make new binding
                  #{(assoc bindings variable literal)}))
  ;; match negation of something
  Negation
  (match-value* [this literal bindings]
               ;; negate literal and match it with contents
               (match-value (:contents this) (neg literal) bindings))
  ;; match all elements in the vector
  clojure.lang.IPersistentVector
  (match-value* [this literal bindings*]
               (loop [elements this
                      bindings #{bindings*}]
                 (if elements
                   (recur (next elements) ; match next element
                          ;; must match current element on all
                          ;; available bindings
                          (set (mapcat #(match-value (first elements) literal %)
                                       bindings)))
                   bindings)))
  ;; match any elements in the set
  clojure.lang.IPersistentSet
  (match-value* [this literal bindings]
               (set (mapcat #(match-value % literal bindings) this))))

;;;; Public API

(defn match-value
  "Returns set of possible bindings after successfull match.
   If match was not successfull, returns nil."
  [value literal bindings]
  (if (= value literal)
    #{bindings}
    (match-value* value literal bindings)))

(comment

  (and
   (= (match-value 1 1 {:d 2})
      #{{:d 2}})

   (= (match-value 1 2 {:d 2})
      nil)  

   (= (match-value 1 (neg 2) {:d 2})
      #{{:d 2}})
   
   (= (match-value (neg 2) 1 {:d 2})
      #{{:d 2}})
   
   (= (match-value (neg 1) 1 {:d 2})
      nil)
   
   (= (match-value 1 (neg 1) {:d 2})
      nil)
   
   (= (match-value :d (neg 4) {:d 2})
      #{{:d [2 (neg 4)]}})

   (= (match-value :x 2 {:x :y})
      #{{:x [:y 2] :y 2}})

   (= (match-value :x 2 {:x (neg 4)})
      #{{:x [(neg 4) 2]}}))

  (match-value :x 2 {:x [:y :z :a] :y (neg 1) :z (neg 3)})

  (match-value :x 2 {:x [2]})

  (match-value :x :y {:x :y})

  (match-value :x 1 {:x :y :y :x})
  
  (match-value :x 1 {:x #{:y :z}})
  
  (match-value :x 1 {:x #{1 (neg 2)}})

  (= Negation (type (neg 1)))

  (when (not (= 1 2)) 4)

)
