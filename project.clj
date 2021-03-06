(defproject piaget "1.0.0-SNAPSHOT"
  :description "Library for interactive analysis of event logs"
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [clj-time "0.3.0-SNAPSHOT"]]
  :dev-dependencies [[swank-clojure "1.3.0-SNAPSHOT"]]
  :java-source-path "java"
  :warn-on-reflection true
  :jvm-opts ["-Dfile.encoding=utf-8"
             "-Dswank.encoding=utf-8"
             "-Xms256m"
             "-Xmx1024m"
             "-XX:MaxPermSize=256m"
             "-Djava.util.logging.config.file=logging.properties"
             #_"-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8030"])
