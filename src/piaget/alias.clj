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

;; helper functions

(defn- create-aliases [next-alias]
  "Create new Aliases instance with supplied next-alias function."
  (Aliases. {} {} nil next-alias))

(defn- add-new
  [{:keys [alias->val val->alias last-alias next-alias] :as old} value]
  "Registers new value if new, returns new aliases."
  (if (contains? val->alias value)
    old ;; value already present
    (let [new-alias (next-alias last-alias)]
      (Aliases.
       (assoc alias->val new-alias value)
       (assoc val->alias value new-alias)
       new-alias
       next-alias))))

;; Create default aliases

;; choose counter alias type
(defonce default-aliases (atom (create-aliases counter-alias)))

;; public API

(defn clear
  "Clears all aliases, use with care!"
  ([] (clear default-aliases))
  ([aliases] (reset! aliases (create-aliases (:next-alias @aliases)))))

(defn resolve
  "Returns value for an alias, nil if alias not found."
  ([alias] (resolve default-aliases alias))
  ([aliases alias] ((:alias->val @aliases) alias)))

(defn alias
  "Registers and returns unique alias for supplied value,
   returns nil if value is nil.
   Do not call within transaction."
  ([value] (alias default-aliases value))
  ([aliases value]
     (io!)
     (when-not (nil? value)
       (or
        ;; optimized for cases where frequent calls are made in which
        ;; value is already added in the aliases
        ((:val->alias @aliases) value)
        ((:val->alias (swap! aliases add-new value)) value)))))
