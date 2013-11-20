(ns jdbc-pg-init.ddl
  (:require [clojure.java.jdbc.sql :as sql]))

(defn create-primary-key
  "Given a table name and a vector of column names, return the DDL
   string for creating a primary key.

   Example:
   (create-primary-key :tablename [:field1 :field2])
   \"ALTER TABLE tablename ADD PRIMARY KEY (field1, field2)\""
  [table-name cols & specs]
  (let [entities-spec (drop-while #(not= :entities %) specs)
        {:keys [entities] :or {entities sql/as-is}} (take 2 entities-spec)
        cols-string (apply str
                           (interpose ", "
                                      (map (sql/as-str entities)
                                           cols)))]
    (format "ALTER TABLE %s ADD PRIMARY KEY (%s)"
            (sql/as-str entities table-name)
            cols-string)))

(defn create-foreign-key
  "Given a foreign key spec, return the DDL string for creating the
   foreign key.

   Examples:
   (create-foreign-key :constraint-name :tablename :field1 :reftablename :reffield1)
   \"ALTER TABLE tablename ADD CONSTRAINT constraint-name FOREIGN KEY (field1) REFERENCES reftablename (reffield1)\"

   (create-foreign-key :ConstraintName :TableName :field1 :RefTableName :reffield1 :entities sql/lower-case)
   \"ALTER TABLE tablename ADD CONSTRAINT constraintname FOREIGN KEY (field1) REFERENCES reftablename (reffield1)\""
  [constraint-name table-name col ref-table-name ref-col & specs]
  (let [entities-spec (drop-while #(not= :entities %) specs)
        {:keys [entities] :or {entities sql/as-is}} (take 2 entities-spec)]
    (format "ALTER TABLE %s ADD CONSTRAINT %s FOREIGN KEY (%s) REFERENCES %s (%s)"
            (sql/as-str entities table-name)
            (sql/as-str entities constraint-name)
            (sql/as-str entities col)
            (sql/as-str entities ref-table-name)
            (sql/as-str entities ref-col))))
