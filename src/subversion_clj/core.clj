(ns subversion-clj.core
  (:require 
    [clojure.string :as string])
  (:import 
     [org.tmatesoft.svn.core.internal.io.fs FSRepositoryFactory FSPathChange]
     [org.tmatesoft.svn.core.internal.io.dav DAVRepositoryFactory]
     [org.tmatesoft.svn.core.internal.io.svn SVNRepositoryFactoryImpl]
     [org.tmatesoft.svn.core.internal.util SVNHashMap SVNHashMap$TableEntry]     
     [org.tmatesoft.svn.core SVNURL SVNLogEntry SVNLogEntryPath SVNException]
     [org.tmatesoft.svn.core.io SVNRepository SVNRepositoryFactory]
     [org.tmatesoft.svn.core.wc SVNWCUtil]
     [java.io File]
     [java.util.LinkedList]))

(declare log-record node-kind)

(DAVRepositoryFactory/setup)
(SVNRepositoryFactoryImpl/setup)
(FSRepositoryFactory/setup)

(defn repo-for
  ^SVNRepository [repo-path]
  (SVNRepositoryFactory/create (SVNURL/parseURIEncoded repo-path)))

(defn logs-for [repo] 
  (->> (.log repo (into-array String []) ^LinkedList (java.util.LinkedList.) 1 -1 true false)
    (map (partial log-record repo))
    (into [])))

(defn log-for [repo revision]
  (let [revision (Long. revision)]
    (first (.log repo (into-array String []) ^LinkedList (java.util.LinkedList.) revision revision true false))))

(defn node-kind [repo path rev]
  (let [basename (.getName (File. ^String path))]
    (if (>= (.indexOf basename ".") 0)
      "file"
      (let [node-kind-at-current-rev (node-kind-at-rev repo path rev)]
        (if (= "none" node-kind-at-current-rev)
          (node-kind-at-rev repo path (- rev 1))
          node-kind-at-current-rev)))))

(defn- node-kind-at-rev ^String [^SVNRepository repo ^String path ^Long rev]
  (.toString (.checkPath repo path rev)))

(defn- normalize-path [path]
  (if (= path "/")
    "/"
    (if (= (first path) \/)
      (apply str (rest path))
      path)))

(defn- change-kind [^FSPathChange change-rec]
  (let [change (.. change-rec getChangeKind toString)
        copy-path (.getCopyPath change-rec)]
    (cond
      copy-path :copy
      (= change "add") :add
      (= change "modify") :edit
      (= change "delete") :delete
      (= change "replace") :replace
      (= change "reset") :reset)))

(defn- detailed-path [repo rev log-record ^SVNHashMap$TableEntry path-record]
  (let [path ^String (normalize-path (.getKey path-record))
        change-rec ^FSPathChange (.getValue path-record)
        node-kind (node-kind repo path rev)
        change-kind (change-kind change-rec)]
    (if (= change-kind :copy)
      [node-kind [path 
                  (normalize-path (.getCopyPath change-rec)) 
                  (.getCopyRevision change-rec)]
       change-kind]
      [node-kind path change-kind])))

(defn- changed-paths [repo rev ^SVNLogEntry log-record]
  (if (= (str rev) "0")
    []
    (map #(detailed-path repo rev log-record %) ^SVNHashMap (.getChangedPaths log-record))))

(defn- log-record [repo ^SVNLogEntry log-obj]
  (let [rev (.getRevision log-obj)
        author (.getAuthor log-obj)
        date (.getDate log-obj)
        message (.getMessage log-obj)
        paths (doall (changed-paths repo rev log-obj))]
    {:revision rev
     :author author
     :time date
     :message (string/trim (str message))
     :changes paths}))