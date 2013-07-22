(ns subversion-clj.wc
  (:require
    [subversion-clj.core :as core]
    [clojure.java.io :as io])
  (:use
    subversion-clj.utils)
  (:import
    [org.tmatesoft.svn.core.wc SVNClientManager SVNWCUtil]
    [org.tmatesoft.svn.core SVNDepth]))

(defn status
  "SVNStatus instance for wc-path. Requires with-client-manager macro around it. Optional argument check-remote? if status should include remote changes."
  ([wc-path]
    (status wc-path false))
  ([wc-path check-remote?]
    (let [cli (.getStatusClient core/*client-manager*)]
      (.doStatus cli (io/as-file wc-path) check-remote?))))

(defn checkout
  "Create a new working copy. Requires with-client-manager macro around it. Optional arguments are recursive? and ignore-externals?"
  ([uri wc-path revision]
     (checkout uri wc-path revision true false))
  ([uri wc-path revision recursive? ignore-externals?]
     (let [cli (.getUpdateClient core/*client-manager*)]
       (.setIgnoreExternals cli ignore-externals?)
       (.doCheckout cli
                    (core/svn-url uri)
                    (io/as-file wc-path)
                    (core/svn-revision revision)
                    (core/svn-revision revision)
                    recursive?))))

(defn switch
  "Switch working copy to different URI. Requires with-client-manager macro around it. Optional arguments are recursive? and ignore-externals?"
  ([uri wc-path revision]
     (switch uri wc-path revision true false))
  ([uri wc-path revision recursive? ignore-externals?]
     (let [cli (.getUpdateClient core/*client-manager*)]
       (.setIgnoreExternals cli ignore-externals?)
       (.doUpdate cli
                  (io/as-file wc-path)
                  (core/svn-revision revision)
                  SVNDepth/INFINITY
                  true
                  true))))

(defn current-revision
  "Current revision of given working copy. Requires with-client-manager macro around it."
  [wc-path]
  (let [status (status wc-path)
        revision (.getRevision status)]
    (when revision
      (.getNumber revision))))
