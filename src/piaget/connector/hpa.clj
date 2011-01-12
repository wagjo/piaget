(ns piaget.connector.hpa
  (:use [clojure.contrib.str-utils :only [str-join]])
  (:require [piaget.connector]
            [clojure.contrib.http.agent :as http]
            [clojure.contrib.json :as json]
            [clojure.java.io :as io]))

(defn- url-encode [text]
  (java.net.URLEncoder/encode text "UTF-8"))

(defn- url-body [body]
  (str-join "&" (map #(str-join "=" (map (comp url-encode name) %)) body)))

(defn- http-post [url body]
  (let [handler #(let [w (java.io.StringWriter.)]
                   (try
                     (let [r (io/reader (http/stream %))]
                       #_(io/copy r w)
                       (json/read-json r))                     
                     (catch Exception e
                       {:handler-error true
                        :response (str w)})))]
    (http/http-agent url
                     :method "POST"
                     :body (url-body body)
                     :handler handler
                     :connect-timeout 6000
                     :read-timeout 6000)))

(deftype Hpa [url password]
  piaget.connector/Connector
  (load-events [this filter]
               (http/result
                (http-post url {:token password
                                :fn "get-activity"
                                :args (json/json-str filter)
                                :timeout "30000"}))))


(comment
  
  (def kplab-connector (Hpa. "http://kplab.tuke.sk:8080/hpa-prod/api/query.jsp" "kplab"))

  (piaget.connector/load-events kplab-connector "30000")

  (def h (http/http-agent "http://kplab.tuke.sk:8080/hpa-prod/api/query.jsp" :method "POST" :body "token=kplab&fn=get-activity&args=30000"))
  
  )
