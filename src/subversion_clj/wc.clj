(ns subversion-clj.wc
  (:require
    [subversion-clj.core :as core]
    [clojure.java.io :as io])
  (:use
    subversion-clj.utils)
  (:import
    [org.tmatesoft.svn.core.wc SVNClientManager SVNWCUtil]
    [org.tmatesoft.svn.core SVNDepth]))

(defn client-manager
  "New SVNClientManager instance. Optional arguments are username and password for authenticated connections."
  ([]
    (SVNClientManager/newInstance (SVNWCUtil/createDefaultOptions true)))
  ([username password]
    (SVNClientManager/newInstance 
      (SVNWCUtil/createDefaultOptions true)
      (core/auth-manager username password))))

(defn status
  "SVNStatus instance for wc-path. Optional argument check-remote? if status should include remote changes."
  ([cli-mgr wc-path]
    (status cli-mgr wc-path false))
  ([cli-mgr wc-path check-remote?]
    (-> cli-mgr
      (.getStatusClient)
      (.doStatus (io/as-file wc-path) check-remote?))))

(defn checkout
  "Create a new working copy. Optional arguments are recursive? and ignore-externals?"
  ([cli-mgr uri wc-path revision]
    (checkout cli-mgr uri wc-path revision true false))
  ([cli-mgr uri wc-path revision recursive? ignore-externals?]
    (let [cli (.getUpdateClient cli-mgr)]
      (.setIgnoreExternals cli ignore-externals?)
      (.doCheckout 
        (core/svn-url uri) 
        (io/as-file wc-path) 
        (core/svn-revision revision) 
        (core/svn-revision revision)
        recursive?))))

(defn switch
  "Switch working copy to different URI. Optional arguments are recursive? and ignore-externals?"
  ([cli-mgr uri wc-path revision]
    (switch cli-mgr uri wc-path revision true false))
  ([cli-mgr uri wc-path revision recursive? ignore-externals?]
    (let [cli (.getUpdateClient cli-mgr)]
      (.setIgnoreExternals cli ignore-externals?)
      (.doSwitch cli 
        (io/as-file wc-path)
        (core/svn-url uri)
        (core/svn-revision revision)
        recursive?))))

(defn update
  "Update existing working copy. Optional argument is ignore-externals?"
  ([cli-mgr wc-path revision]
    (update cli-mgr wc-path revision false))
  ([cli-mgr wc-path revision ignore-externals?]
    (let [cli (.getUpdateClient cli-mgr)]
      (.setIgnoreExternals cli ignore-externals?)
      (.doUpdate
        (io/as-file wc-path)
        (core/svn-revision revision)
        SVNDepth/INFINITY
        true
        true))))