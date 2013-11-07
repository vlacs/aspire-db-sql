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

(defn create!
  [db ddl-fn & specs]
  (try
    (jdbc/db-do-commands db true (apply ddl-fn specs))
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

(defn create-indices!
  "Create the specified indices."
  [db table-name indices]
  (doall (for [index-name (keys indices)]
           (apply create!
                  db
                  ddl/create-index
                  index-name
                  table-name
                  (indices index-name)))))

(defn create-foreign-keys!
  "Create the specified FKs."
  [db table-name fks]
  (doall (for [fk (keys fks)]
           (apply create! db ddl/create-foreign-key fk table-name (fks fk)))))

(defn init!
  "Ensure that all the tables in application schema exist in the local DB"
  [db schema]
  (let [schema-tables (:tables schema)
        missing-tables (missing-tables db schema-tables)
        fk-fns (atom [])]
    (doall (for [t missing-tables]
             (let [schema (schema-tables t)
                   cols (schema :cols)
                   pk (schema :primary-key)
                   fks (schema :foreign-keys)
                   indices (schema :indices)]
               (apply create! db ddl/create-table t cols)
               (if pk (create! db ddl/create-primary-key t pk))
               (swap! fk-fns conj (delay (create-foreign-keys! db t fks)))
               (create-indices! db t indices))))
    ;; The tables might be created out of order, so delay all foreign
    ;; keys until last.
    (doall (map #(deref %) @fk-fns)))
  nil)

(comment
  ; # lein with-profile dev deps
  (require '[clojure.edn :as edn])
  (def schema (edn/read-string (slurp "sample-schema.edn")))
  (def db (edn/read-string (slurp "sample-config.edn")))
  (def db (assoc db :password "some real password" :subname "//real-hostname/real-db"))
  (init! db schema)
  )

