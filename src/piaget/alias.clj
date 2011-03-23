;; Copyright (C) 2011, Jozef Wagner. All rights reserved. 

(ns piaget.alias
  "Provide unique alias for value of any type.
   Purpose of this is to consume less memory when storing large
   amounts of data were these values are frequently used."
  (:refer-clojure :exclude [resolve alias]))

;; Bidirectional map for storing values and its aliases
;; NOTE: defrecord should yield faster lookup than simple map
(defrecord Aliases [alias->val val->alias last-alias next-alias])

;; Simple incremental counter alias

(defn- counter-alias [last-alias]
  "Use incremental counter for generating aliases."
  (if (number? last-alias)
    (inc last-alias)
    0))

;; Custom alias generation functions should accept one parameter,
;; last-alias, which contains last generated alias or nil,
;; if alias is requested for the first time

;;;; Public API

(defn create-aliases
  "Creates new Aliases instance with supplied next-alias function,
  or use counter-alias as next-alias function."
  ([]
     (create-aliases counter-alias))
  ([next-alias]
     (Aliases. {} {} nil next-alias)))

(defn add-new
  "Registers new value if new, returns new aliases."
  [{:keys [alias->val val->alias last-alias next-alias] :as old} value]
  (if (contains? val->alias value)
    old ;; value already present
    (let [new-alias (next-alias last-alias)]
      (Aliases.
       (assoc alias->val new-alias value)
       (assoc val->alias value new-alias)
       new-alias
       next-alias))))

(defn resolve
  "Returns value for an alias, nil if alias not found."
  [aliases alias]
  ((:alias->val aliases) alias))

(defn alias
  "Returns unique alias for supplied value,
   returns nil if value was not aliased."
  [aliases value]
  ((:val->alias aliases) value))

(comment

  (def a (add-new (create-aliases) :value))
  (alias a :value)
  
)
