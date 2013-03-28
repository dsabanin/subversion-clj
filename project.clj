(defproject subversion-clj "0.1"
  :description "SVNKit based Subversion API for Clojure"
  :dependencies [[org.tmatesoft.svnkit/svnkit "1.7.8"]]
  :dev-dependencies [[midje "1.4.0"]
                     [com.stuartsierra/lazytest "1.2.3"]
                     [lein-marginalia "0.7.1"]
                     [lein-clojars "0.9.0"]
                     [lein-midje "1.0.10"]]
  :repositories {"tmatesoft" "http://maven.tmatesoft.com/content/repositories/releases/"
                 "stuartsierra-releases" "http://stuartsierra.com/maven2"})
