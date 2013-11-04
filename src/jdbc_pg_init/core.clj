(ns
    ^{:author "Matt Oquist <moquist@vlacs.org>"
      :doc
      "A side-effects library to create PostgreSQL tables and indices
       from edn data.
       TODO:
         * Handle versioning of schema and application of deltas to DB schema.
         * Make index creation smarter... right now it's all or nothing."}
    jdbc-pg-init.core
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.ddl :as ddl]
            [clojure.java.jdbc.sql :as sql]
            [clojure.set :as set]))

;;; see http://dev.clojure.org/jira/browse/JDBC-53
;;; Deprecate this and use the new jdbc function(s) when they're released.
(defn create!
  [db ddl-fn name & specs]
  (try
    (jdbc/db-do-commands db true (apply ddl-fn name specs))
    (catch Exception e
      (.printStackTrace (.getCause e)))))

(defn get-tables
  "Get the tables and views that are already defined in the specified db.
   Return the set of table/view names.
   See http://clojure.github.io/java.jdbc/doc/clojure/java/jdbc/UsingDDL.html"
  [db]
  (with-open [dbconn (jdbc/get-connection db)]
    (into #{}
          (map #(keyword (:table_name %))
               (jdbc/result-set-seq
                (-> (.getMetaData dbconn)
                    (.getTables nil nil nil (into-array ["TABLE" "VIEW"]))))))))

(defn missing-tables
  "Get set of tables in application schema that do not exist in the local DB"
  [db schema-tables]
  (let [tables (get-tables db)]
    (set/difference (into #{} (keys schema-tables)) tables)))

(defn ensure-tables!
  "Ensure that all the tables in application schema exist in the local DB"
  [db schema]
  (let [schema-tables (:tables schema)
        missing-tables (missing-tables db schema-tables)]
    (doall (for [t missing-tables]
             (and (apply create! db ddl/create-table t (schema-tables t))
                  t)))))

(defn create-indices!
  "Create the specified indices."
  [db schema]
  (let [schema-indices (:indices schema)]
    (doall (for [key (keys schema-indices)]
             (apply create! db ddl/create-index key (schema-indices key))))))

(defn init!
  "Ensure that all necessary tables and all indices from schema exist in the specified DB."
  [db schema]
  (ensure-tables! db schema)
  (create-indices! db schema)
  nil)

(comment
  ; # lein with-profile dev deps
  (require '[clojure.edn :as edn])
  (def schema (edn/read-string (slurp "sample-schema.edn")))
  (def db (edn/read-string (slurp "sample-config.edn")))
  (def db (assoc db :password "some real password" :subname "//real-hostname/real-db"))
  (init! db schema)
  )
