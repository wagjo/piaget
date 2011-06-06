;; Copyright (C) 2011, Jozef Wagner. All rights reserved. 

(ns piaget.connector.wiki
  "Comparison types"
  (:use clojure.contrib.lazy-xml
        [clojure.contrib.logging :only (info debug warn error)])
  (:require [clojure.contrib.duck-streams :as ds]
            clojure.contrib.string
            clojure.java.io
            piaget.connector)
  (:import java.io.FileInputStream
           java.io.BufferedInputStream
           java.io.File
           java.util.zip.GZIPInputStream
           piaget.ExternalSort
           java.util.Comparator))

(defn to-int [s]
  (try
    (Integer/parseInt s)
    (catch Exception e s)))

(defn xml-from-gz-file [^String filename]
  (let [f (GZIPInputStream. (BufferedInputStream. (FileInputStream. filename)))]
    (parse-trim f startparse-sax 1)))

(defn page-seq [history]
  (filter #(= :page (:tag %)) (:content history)))

(defn page-property-seq [page]
  (remove #(= :revision (:tag %)) (:content page)))

(defn history-seq [page]
  (filter #(= :revision (:tag %)) (:content page)))

(defn parse-contributor [e p]
  (condp = (:tag p)
      :id (assoc e :actor (to-int (first (:content p))))
      :username (assoc e :actor-name (first (:content p)))
      :ip (assoc e :actor-ip (first (:content p)))
      (assoc e (:tag p) :unknown-contributor-tag)))

(defn parse-text [e [k v]]
  (condp = k
      :id (assoc e :text-id (to-int v))
      :bytes (assoc e :text-bytes (to-int v))
      :deleted (assoc e :text-deleted true)
      (assoc e k :unknown-text-tag)))

(defn parse-comment [e-old c]
  (when c
    (let [e (assoc e-old :title c)
          re-compile #(java.util.regex.Pattern/compile % java.util.regex.Pattern/CASE_INSENSITIVE) ]
      (cond
       (and (re-find (re-compile "revert|\bund(o|id)|\brvv?") c)
            (re-find (re-compile "by \\[\\[") c))
       (let [link (last (re-find #"by \x5b\x5b([^\x5d]*)\x5d\x5d" c))
             culprit (if link
                       (last (clojure.contrib.string/partition #"\x7c|\x2f|\x3a" link))
                       nil)]
         (merge e {:revert true
                   :culprit culprit}))
       :else e))))

(defn parse-revision-property [e p]
  (condp = (:tag p)
      :id (assoc e :id (to-int (first (:content p))))
      :timestamp (assoc e :start (first (:content p)))
      :contributor (reduce parse-contributor e (:content p))
      :minor (assoc e :minor true)
      :comment (parse-comment e (first (:content p)))
      :text (reduce parse-text e (:attrs p))
      (assoc e (:tag p) :unknown)))

(defn parse-page-property [e p]
  (condp = (:tag p)
      :id (assoc e :entity (to-int (first (:content p))))
      :title (assoc e :entity-title (first (:content p)))
      :redirect (assoc e :redirect true)
      (assoc e (:tag p) :unknown)))

(defn event-from-revision [rev blueprint-event]
  (reduce parse-revision-property blueprint-event (:content rev)))

(defn create-blueprint-event [page-properties]
  (reduce parse-page-property {} page-properties))

(defn page-events-seq [page]
  (let [blueprint-event (create-blueprint-event (page-property-seq page))]
    (map #(event-from-revision % blueprint-event) (history-seq page))))

(defn event-seq [history]
  (mapcat page-events-seq (page-seq history)))

(defn dump-reverts [events]
  (ds/write-lines "reverts.clj"
                  (map #(select-keys % [:id :title :start
                                        :entity :entity-title
                                        :actor :actor-name :culprit])
                       (filter :revert events))))

(defn restore-dump [filename]
  (let [f (java.io.PushbackReader. (clojure.java.io/reader filename))]
    (take-while
     #(not= ::eof %)
     (repeatedly #(read f false ::eof)))))

(defn process-log []
  (dump-reverts (event-seq (xml-from-gz-file "wiki.xml.gz"))))

(def my-comp (proxy [Comparator] []
               (compare [^String s1 ^String s2]
                        (let [t1 (re-find #":start \x22([^\x22]*)\x22" s1)
                              t2 (re-find #":start \x22([^\x22]*)\x22" s2)]
                          (.compareTo t1 t2)))))

(defn sort-dump []
  (let [l (ExternalSort/sortInBatch (File. "reverts-complete.clj") my-comp)]
    (ExternalSort/mergeSortedFiles l (File. "output.clj") my-comp)))

(deftype Wiki [filename]
  piaget.connector/Connector
  (load-events [this filter]
               ;; filter is ignored
               (restore-dump filename))
  (resource-name [this ids]
                 (throw (UnsupportedOperationException. "not available"))))

(comment
  (process-log)
  (sort-dump)
  (error "ahoj")
  (crash)
  (def e (event-seq (xml-from-gz-file "wiki.xml.gz")))
  (count e)
  (dump-reverts (take 10000 e))
  (:id  (second  (restore-dump "reverts-sorted.clj")))
  (def x (restore-dump "reverts-sorted.clj"))
  (def xx (doall x))
  (let [f (GZIPInputStream. (BufferedInputStream. (FileInputStream. "wiki.xml.gz")))]
    (count (take 10 (parse-seq f startparse-sax 1))))  
  (last (clojure.contrib.string/partition #"\x7c|\x2f|\x3a" (last (re-find #"by \x5b\x5b([^\x5d]*)\x5d\x5d" "aa by [[asdoj / 23434|sdasdkpo 123/123.1212.3.23]] sdpasdk"))))
  (clojure.contrib.string/partition #"\x7c|\x5b|\x5d" "aaa][b:bb]")
  #"\x5b"
  )
;; (take 300 (map #(count (:content %)) (:content (load-wiki))))
;; (count (:content (nth (:content (load-wiki)) 3)))
;; (remove #(or (nil? %) (nil? (re-find (java.util.regex.Pattern/compile "revert|\bund(o|id)|\brvv?" java.util.regex.Pattern/CASE_INSENSITIVE) %))) (take 200 (map :title (page-events-seq (nth p 1)))))
