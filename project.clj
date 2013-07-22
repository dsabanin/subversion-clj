(defproject subversion-clj "0.3.13"
  :description "SVNKit based Subversion API for Clojure"
  :dependencies [[org.tmatesoft.svnkit/svnkit "1.7.9"]
                 [org.apache.commons/commons-io "1.3.2"]
                 [org.clojars.dsabanin/clj-det-enc "1.0.0"]
                 [org.clojure/core.typed "0.1.17"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.5.1"]
                                  [midje "1.5.1"]
                                  [com.stuartsierra/lazytest "1.2.3"]
                                  [lein-marginalia "0.7.1"]]
                   :plugins [[lein-midje "3.0.1"]
                             [lein-marginalia "0.7.1"]]}}
  :repositories {"tmatesoft" "http://maven.tmatesoft.com/content/repositories/releases/"
                 "stuartsierra-releases" "http://stuartsierra.com/maven2"}
  :aot [subversion-clj.diff-generator]
  :jvm-opts ["-Djava.awt.headless=true"])
