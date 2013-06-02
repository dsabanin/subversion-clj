(ns subversion-clj.diff-generator
  (:use
    subversion-clj.utils)
  (:require
    [hozumi.det-enc :as enc]
    [clojure.string :as string])
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

(defn -construct
  []
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
  ([^StructuredDiffGenerator this os path deleted]
     false)
  ([^StructuredDiffGenerator this type path copyFromPath copyFromRevision result]
     (update-change-type this path (type->keyword type))
     false))

(def binary? (partial = "application/octet-stream"))

(def text? (complement binary?))

(defn text-diff
  [^StructuredDiffGenerator generator path file1 file2 rev1 rev2 mime1 mime2]
  (let [bs (baos)
        encoding (or (enc/detect (baos->bais bs)) (.getEncoding generator))]
    (.parentDisplayFileDiff generator path file1 file2 rev1 rev2 mime1 mime2 bs)
    (.toString bs encoding)))

(defn set-diff!
  [^StructuredDiffGenerator generator path s]
  (swap! (.state generator) assoc-in [:diffs :files path] s))

(defn -displayFileDiff
  [^StructuredDiffGenerator this path file1 file2 rev1 rev2 mime1 mime2 os]
  (let [path (normalize-path path)]
    (if (every? text? [mime1 mime2])
      (set-diff! this  path (text-diff this path file1 file2 rev1 rev2 mime1 mime2))
      (set-diff! this path "Can't compare binary files."))))

(defn text-property
  [^StructuredDiffGenerator generator path baseProps diff]
  (let [bs (baos)
        encoding (or (enc/detect (baos->bais bs)) (.getEncoding generator))]
    (.parentDisplayPropDiff generator path baseProps diff bs)
    (string/trim (.toString bs encoding))))

(defn set-property!
  [^StructuredDiffGenerator generator path s]
  (swap! (.state generator) assoc-in [:diffs :properties path] s))

(defn -displayPropDiff
  [^StructuredDiffGenerator this path baseProps diff os]
  (let [path (normalize-path path)]
    (set-property! this path (text-property this path baseProps diff))))

(defn -grabDiff
  [^StructuredDiffGenerator this]
  (:diffs @(.state this)))

(defn -grabFileChanges
  [^StructuredDiffGenerator this]
  (:changes @(.state this)))
