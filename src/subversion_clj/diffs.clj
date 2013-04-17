(ns subversion-clj.diffs
  (:use
    subversion-clj.utils)
  (:import 
    [org.tmatesoft.svn.core.wc.admin ISVNGNUDiffGenerator]
    [org.tmatesoft.svn.core.internal.wc DefaultSVNGNUDiffGenerator]
    [java.io ByteArrayOutputStream])
  (:gen-class
    :name "subversion.diffs.StructuredDiffGenerator"
    :implements [org.tmatesoft.svn.core.wc.admin.ISVNGNUDiffGenerator]
    :exposes-methods {displayFileDiff parentDisplayFileDiff
                      displayPropDiff parentDisplayPropDiff}
    :extends org.tmatesoft.svn.core.internal.wc.DefaultSVNGNUDiffGenerator
    :init construct
    :state "state"
    :methods [[grabDiff [] clojure.lang.IObj]]))

(defn -construct []
  [[] (atom {})])

(defn -displayHeader 
  ([this os path deleted] false)
  ([this type path copyFromPath copyFromRevision result] false))

(defn -displayFileDiff
  [^subversion.diffs.StructuredDiffGenerator this path file1 file2 rev1 rev2 mime1 mime2 os]
  (let [bs (baos)
        path (normalize-path path)]
    (.parentDisplayFileDiff this path file1 file2 rev1 rev2 mime1 mime2 bs)
    (swap! (.state this) assoc-in [:files path] bs)))

(defn -displayPropDiff
  [^subversion.diffs.StructuredDiffGenerator this path baseProps diff os]
  (let [bs (baos)
        path (normalize-path path)]
    (.parentDisplayPropDiff this path baseProps diff bs)
    (swap! (.state this) assoc-in [:properties path] bs)))

(defn -grabDiff
  [^subversion.diffs.StructuredDiffGenerator this]
  @(.state this))