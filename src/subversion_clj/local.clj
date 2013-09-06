;; ## Local FS access to Subversion repos
;;
;; This allows you to get fast access to a repo stored locally. The same way svnadmin and svnlook do.

(ns subversion-clj.local
  (:require
    [subversion-clj.core :as core]
    subversion-clj.diff-generator)
  (:use
    subversion-clj.utils)
  (:import
    [org.tmatesoft.svn.core.io SVNRepository]
    [subversion.clj StructuredDiffGenerator]
    [java.io File ByteArrayOutputStream]
    [org.tmatesoft.svn.core.wc SVNWCUtil SVNClientManager SVNRevision SVNDiffOptions]
    [org.tmatesoft.svn.core.wc.admin ISVNGNUDiffGenerator SVNLookClient]))

(defn svnlook-client
  "Return SVNLookClient instance. Requires with-client-manager macro around it."
  ^SVNLookClient []
  (.getLookClient core/*client-manager*))

(defn repo-dir
  "File instance for a repository directory."
  ^File [^SVNRepository repo]
  (File. (.. repo getLocation getPath)))

(defn diff-for
  "File and property changes for a given revision. Returns a single ByteArrayOutputStream instance.

_Works only with repo object pointing to a local repo directory (not working copy)._"
  ([^SVNRepository repo revision]
    (diff-for repo revision {:deleted true :added true :copy-form false}))
  ([^SVNRepository repo revision options]
    (core/with-client-manager
    (let [output (baos)]
      (.doGetDiff (svnlook-client) (repo-dir repo) (core/svn-revision revision) (:deleted options) (:added options) (:copy-form options) output)
      output))))

(defn diff-for!
  "File and property changes for a given revision. Writes changes into a generator instance.

_Works only with repo object pointing to a local repo directory (not working copy)._"
  ([^SVNRepository repo revision ^ISVNGNUDiffGenerator generator]
    (diff-for! repo revision generator {:deleted true :added true :copy-form false}))
  ([^SVNRepository repo revision ^ISVNGNUDiffGenerator generator options]
    (core/with-client-manager
    (doto (svnlook-client)
      (.setDiffGenerator generator)
      (.doGetDiff (repo-dir repo) (core/svn-revision revision) (:deleted options) (:added options) (:copy-form options) null-stream)))
  generator))

(defn diff-options
  [ignore-whitespace? ignore-amount-whitespace? ignore-eol-style?]
  (SVNDiffOptions. ignore-whitespace? ignore-amount-whitespace? ignore-eol-style?))

(defn structured-generator
  ([]
    (structured-generator false))
  ([ignore-whitespace?]
    (structured-generator ignore-whitespace? nil))
  ([ignore-whitespace? external-diff-command]
    (doto (StructuredDiffGenerator.)
     (.setEncoding "UTF-8")
     (.setDiffOptions (diff-options ignore-whitespace? false false))
     (.setExternalDiffCommand external-diff-command))))

(defn structured-diff-for
  "File and property changes for a given revision, structured as maps of maps.

   Format of the returned map is:

         {:files
            {\"dir-a/file1\" ByteArrayOutputStream
             \"dir-b/file2\" ByteArrayOutputStream}
          :properties
            {\"dir-a/file1\" ByteArrayOutputStream}}

_Works only with repo object pointing to a local repo directory (not working copy)._"
  ([^SVNRepository repo revision]
    (structured-diff-for repo revision false))

  ([^SVNRepository repo revision ignore-whitespace?]
    (let [generator (structured-generator ignore-whitespace?)]
      (diff-for! repo revision generator)
      (.grabDiff generator))))
