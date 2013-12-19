(defproject jdbc-pg-init "0.1.2"
  :description "A library to create PostgreSQL tables and indices from edn data."
  :url "https://github.com/vlacs/jdbc-pg-init"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/java.jdbc "0.3.0-beta1"]
                 [postgresql/postgresql "8.4-702.jdbc4"]])
