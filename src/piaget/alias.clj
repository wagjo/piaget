(ns piaget.alias
  "provide unique alias for value of any type.
   Purpose of this is to consume less memory when storing large
   amounts of data were these values are frequently used"
  (:refer-clojure :exclude [resolve alias]))

;; Simple incremental counter alias

(defn- counter-alias [last-alias]
  "Use incremental counter for generating aliases"
  (if (number? last-alias)
    (inc last-alias)
    0))

;; Custom alias generation functions should accept one parameter,
;; last-alias, which contains last generated alias or nil,
;; if alias is requested for the first time

(def next-alias counter-alias) ;; choose counter alias type

;; Define bidirectional map for storing objects and its aliases

;; NOTE: defrecord should yield faster lookup than simple map
(defrecord Aliases [alias->val val->alias last-alias])

(def initial-aliases (Aliases. {} {} nil))

(defonce aliases (atom initial-aliases))

;; helper functions

(defn- add-new [{:keys [alias->val val->alias last-alias] :as old} value]
  "Registers new value if new"
  (if (contains? val->alias value)
    old
    (let [new-alias (next-alias last-alias)]
      (Aliases.
       (assoc alias->val new-alias value)
       (assoc val->alias value new-alias)
       new-alias))))

;; public API

(defn clear []
  "Clears all aliases, use with care!"
  (reset! aliases initial-aliases))

(defn resolve [alias]
  "Returns value for an alias, nil if alias not valid"
  ((:alias->val @aliases) alias))

(defn alias [value]
  "Registers and returns unique alias for supplied value,
   returns nil if value is nil"
  (when-not (nil? value)
    ((:val->alias (swap! aliases add-new value)) value)))
