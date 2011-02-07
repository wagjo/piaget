;;
;; Copyright (C) 2011, Jozef Wagner
;;
;; This file is part of Piaget.
;; 
;; Piaget is free software: you can redistribute it and/or modify
;; it under the terms of the GNU General Public License as published by
;; the Free Software Foundation, either version 3 of the License, or
;; (at your option) any later version.
;;
;; Piaget is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;; GNU General Public License for more details.
;; 
;; You should have received a copy of the GNU General Public License
;; along with Piaget.  If not, see <http://www.gnu.org/licenses/>.
;;
;; This work is supported by the KP-Lab project <http://kp-lab.org/>,
;; which is supported by European Commission DG INFSO under the IST program,
;; contract No. 27490.
;;

(ns piaget.connector
  "bla bla")

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
            in time range
  - see source for examples")
  (resource-name [this ids]
 "Get a last known name for resources based on their ids.
  Parameter ids can be a single id or seq of ids."))

;; NOTE: do we need support for logical AND in filter?
;; TODO: use joda time and update docstring and example
;; TODO: Why [this id & ids] is not supported in defprotocol?

(comment
  ;; filter example
  (def f1 {:time ["2009-02-03 15:30:00" nil]
           :type #{"creation" "modification"}
           :actor "some-actor-unique-identified"
           :from 30
           :count 10})
)
