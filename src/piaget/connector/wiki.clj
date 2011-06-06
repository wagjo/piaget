;; Copyright (C) 2011, Jozef Wagner. All rights reserved. 

(ns piaget.connector.wiki
  "Connector for wikipedia edit history dump"
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

;;;; Translation to event

(defn- to-int [s]
  "Converts string to int.
  If conversion is not successfull, returns original string."
  (try
    (Integer/parseInt s)
    (catch Exception e s)))

(defn- parse-contributor [p]
  "Parses contributor XML element into map."
  (let [c (first (:content p))]
    (condp = (:tag p)
        :id [:actor (to-int c)]
        :username [:actor-name c]
        :ip [:actor-ip c]
        [(:tag p) :unknown-contributor-tag])))

(defn- parse-text [[k v]]
  "Parses attribute of text XML element into map."
  (condp = k
      :id [:text-id (to-int v)]
      :bytes [:text-bytes (to-int v)]
      :deleted [:text-deleted true]
      [k :unknown-text-tag]))

(defn- parse-comment [c]
  "Parse comment string."
  (when c
    (let [r [:title c]
          re-compile #(java.util.regex.Pattern/compile % java.util.regex.Pattern/CASE_INSENSITIVE)
          found-nice-revert (and (re-find (re-compile "revert|\bund(o|id)|\brvv?") c)
                                 (re-find (re-compile "by \\[\\[") c))]
      (if found-nice-revert
        (let [link (last (re-find #"by \x5b\x5b([^\x5d]*)\x5d\x5d" c))
              culprit (when link
                        (last (clojure.contrib.string/partition #"\x7c|\x2f|\x3a" link)))]
          (concat r [:revert true
                     :culprit culprit]))
        r))))

(defn- parse-revision-property [p]
  "Parse revision XML element into map."
  (let [c (first (:content p))]
    (condp = (:tag p)
        :id [:id (to-int c)]
        :timestamp [:start c]
        :minor [:minor true]
        :comment (parse-comment c)
        :text (mapcat parse-text (:attrs p))
        :contributor (mapcat parse-contributor (:content p))
        [(:tag p) :unknown])))

(defn- parse-page-property [p]
  "Parse global properties of page XML element."
  (let [c (first (:content p))]
    (condp = (:tag p)
        :id [:entity (to-int (first (:content p)))]
        :title [:entity-title (first (:content p))]
        :redirect [:redirect true]
        [(:tag p) :unknown])))

;;;; XML processing

(defn xml-from-gz-file
  "Loads gziped XML file into the lazy tree."
  [^String filename]
  (let [f (GZIPInputStream. (BufferedInputStream. (FileInputStream. filename)))]
    (parse-trim f startparse-sax 1)))   ; do not read ahead

(defn page-seq [history]
  "Creates lazy seq of page XML elements from wikipedia edit history."
  (filter #(= :page (:tag %)) (:content history)))

(defn page-property-seq [page]
  "Returns lazy seq of page properties for given page XML element."
  (remove #(= :revision (:tag %)) (:content page)))

(defn history-seq [page]
  "Returns lazy seq of page revisions for given page XML element."
  (filter #(= :revision (:tag %)) (:content page)))

(defn page-events-seq [page]
  "Returns lazy seq of events for given page XML element"
  (let [e (apply hash-map (mapcat parse-page-property (page-property-seq page)))
        event-from-revision (fn [rev] (merge e
                                            (apply hash-map
                                                   (mapcat parse-revision-property (:content rev)))))]
    (map event-from-revision (history-seq page))))

(defn event-seq [history]
  "Returns lazy seq of events from wikipedia edit history"
  (mapcat page-events-seq (page-seq history)))

;;;; Dumping events

(defn dump-reverts [events filename]
  "Dumps revert events into file."
  (ds/write-lines "reverts.clj"
                  (map #(select-keys % [:id :title :start
                                        :entity :entity-title
                                        :actor :actor-name :culprit])
                       (filter :revert events))))

(defn dumped-events-seq [filename]
  "Returns lazy seq of events from dump file"
  (let [f (java.io.PushbackReader. (clojure.java.io/reader filename))]
    (take-while
     #(not= ::eof %)
     (repeatedly #(read f false ::eof)))))

;;;; Sorting

;; Custom coparator for dumped events


(defn sort-dump
  "Sorts dumped events"
  [from to]
  (let [my-comp (proxy [Comparator] []
                  (compare [^String s1 ^String s2]
                           (let [t1 (re-find #":start \x22([^\x22]*)\x22" s1)
                                 t2 (re-find #":start \x22([^\x22]*)\x22" s2)]
                             (.compareTo t1 t2))))
        l (ExternalSort/sortInBatch (File. from) my-comp)]
    (ExternalSort/mergeSortedFiles l (File. to) my-comp)))

;;;; Connector

(deftype Wiki [filename]
  piaget.connector/Connector
  (load-events [this filter]
               ;; filter is ignored
               (dumped-events-seq filename))
  (resource-name [this ids]
                 (throw (UnsupportedOperationException. "not available"))))

(comment
  ;;;; Create dump
  (dump-reverts (event-seq (xml-from-gz-file "wiki.xml.gz")) "reverts.clj")
  )
