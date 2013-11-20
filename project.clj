(defproject jdbc-pg-init "0.1.1-SNAPSHOT1"
  :description "A library to create PostgreSQL tables and indices from edn data."
  :url "https://github.com/vlacs/jdbc-pg-init"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/java.jdbc "0.3.0-beta1"]]
  :profiles
  {:dev
   {:dependencies [[postgresql/postgresql "8.4-702.jdbc4"]
                   [mysql/mysql-connector-java "5.1.26"]]}})
