(defproject etl-cascalog "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :profiles {:provided {:dependencies [[org.apache.hadoop/hadoop-client "2.7.3"]]}}
  :repositories {"sonatype-oss-public" "http://conjars.org/repo/"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.taoensso/timbre "4.7.4"]
                 [com.fzakaria/slf4j-timbre "0.3.2"]
                 [cheshire "5.6.3"]
                 [org.clojure/java.jdbc "0.7.0-alpha1"]
                 [org.postgresql/postgresql "9.4.1212"]
                 [cascalog/cascalog-core "3.0.0"]
                 [cascading/cascading-jdbc-postgresql "2.5.5" :exclusions [postgresql org.postgresql/postgresql]]
                 #_[cascading/cascading-jdbc-postgresql "3.0.0" :exclusions [postgresql org.postgresql/postgresql]]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [org.clojure/math.combinatorics "0.1.4"]
                 [clj-time "0.12.2"]
                 [clojurewerkz/balagan "1.0.5" :exclusions [org.clojure/clojure]]
                 [cascalog/cascalog-more-taps "3.0.0"]])
