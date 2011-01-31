(ns piaget.event)

;; TODO: defrecord docstring???

(defrecord Event [id start])
"Each event must contain following keys:
   :id - unique id of an event, if source data does not contain ids,
         connector must generate one for each event
   :start - event start time
  Other recommended keys are:
   :end - event end time, does not have to be defined if event has
          no duration
   :type - type of event
   :actor - id of user who performed the event, subject of the event
   :entity - id of entity which was object of the event
   :title - title/short description of the event
   :desc - long description of the event
   :belongs-to - id of some shared space under which the event was
                 performed
  Less used keys:
   :actor-name, :actor-type
   :entity-type, :entity-title
   :refers-to - id of second entity related to the event (first
                being the :entity). Used e.g. in event which link
                two entities
   :tool - name of end-user tool which generated the event
   :children - for grouping events, specify list of children
  Event value can be one value (string, number, date, ...) or seq
   of values"
