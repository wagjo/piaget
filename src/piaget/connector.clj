(ns piaget.connector)

(defprotocol Connector
  (load-events [this filter]))
