(ns subversion-clj.diff-generator
  (:use
    subversion-clj.utils)
  (:import 
    [org.tmatesoft.svn.core.wc.admin ISVNGNUDiffGenerator]
    [org.tmatesoft.svn.core.internal.wc DefaultSVNGNUDiffGenerator]
    [java.io ByteArrayOutputStream]
    [subversion.clj StructuredDiffGenerator])
  (:gen-class
    :name "subversion.clj.StructuredDiffGenerator"
    :implements [org.tmatesoft.svn.core.wc.admin.ISVNGNUDiffGenerator]
    :exposes-methods {displayFileDiff parentDisplayFileDiff
                      displayPropDiff parentDisplayPropDiff}
    :extends org.tmatesoft.svn.core.internal.wc.DefaultSVNGNUDiffGenerator
    :init construct
    :state "state"
    :methods [[grabDiff [] clojure.lang.IObj]
              [grabFileChanges [] clojure.lang.IObj]]))

(defn -construct []
  [[] (atom {:diffs {:files {}
                     :properties {}}
             :changes {}})])

(def type->keyword
  {ISVNGNUDiffGenerator/ADDED :add
   ISVNGNUDiffGenerator/COPIED :copy
   ISVNGNUDiffGenerator/DELETED :delete
   ISVNGNUDiffGenerator/MODIFIED :edit})

(defn update-change-type
  [^StructuredDiffGenerator generator path type]
  (let [path (normalize-path path)]
    (swap! (.state generator) assoc-in [:changes path] type)))

(defn -displayHeader 
  ([^StructuredDiffGenerator this os path deleted] false)
  ([^StructuredDiffGenerator this type path copyFromPath copyFromRevision result]
    (update-change-type this path (type->keyword type))
    false))

(defn -displayFileDiff
  [^StructuredDiffGenerator this path file1 file2 rev1 rev2 mime1 mime2 os]
  (let [bs (baos)
        path (normalize-path path)]
    (.parentDisplayFileDiff this path file1 file2 rev1 rev2 mime1 mime2 bs)
    (swap! (.state this) assoc-in [:diffs :files path] bs)))

(defn -displayPropDiff
  [^StructuredDiffGenerator this path baseProps diff os]
  (let [bs (baos)
        path (normalize-path path)]
    (.parentDisplayPropDiff this path baseProps diff bs)
    (swap! (.state this) assoc-in [:diffs :properties path] bs)))

(defn -grabDiff
  [^StructuredDiffGenerator this]
  (:diffs @(.state this)))

(defn -grabFileChanges
  [^StructuredDiffGenerator this]
  (:changes @(.state this)))
