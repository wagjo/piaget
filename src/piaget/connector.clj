;; Copyright (C) 2011, Jozef Wagner. All rights reserved. 

(ns piaget.connector
  "Protocol for connectors. Connectors fetch events from repositories")

(defprotocol Connector
  (load-events [this filter]
 "Load events from <this> data source based on specified <filter>.
  Returns (possibly lazy) seq of events, ordered by start time.
  <filter> is a map
  - Connector must support all required keys as specified in the
    event type. Moreover, following keys must be supported:
    :from  - limit returned results, skip specified number of results
    :count - limit number of returned results to specified value
    :time  - time range in which events started
  - Following collections have specific meaning if used as value:
    #{}   - search for any of specified values (logical OR)
    [X Y] - search for values in a given X Y range, used e.g.
            in time range")
  (resource-name [this ids]
 "Get a last known name for resources based on their ids.
  Parameter ids can be a single id or seq of ids."))

;; NOTE: do we need support for logical AND in filter?
;; TODO: use joda time and update docstring and example

(comment
  ;; filter example
  (def f1 {:time ["2009-02-03 15:30:00" nil]
           :type #{"creation" "modification"}
           :actor "some-actor-unique-identified"
           :from 30
           :count 10})
)
