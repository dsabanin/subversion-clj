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
  ^SVNLookClient []
  (let [opts (SVNWCUtil/createDefaultOptions true)
        cm (SVNClientManager/newInstance opts)]
    (.getLookClient cm)))

(defn repo-dir
  "File instance for a repository directory."
  ^File [^SVNRepository repo]
  (File. (.. repo getLocation getPath)))

(defn diff-for
  "File and property changes for a given revision. Returns a single ByteArrayOutputStream instance.

_Works only with repo object pointing to a local repo directory (not working copy)._"
  [^SVNRepository repo revision]
  (let [output (baos)]
    (.doGetDiff (svnlook-client) (repo-dir repo) (core/svn-revision revision) true true true output)
    output))

(defn diff-for!
  "File and property changes for a given revision. Writes changes into a generator instance.

_Works only with repo object pointing to a local repo directory (not working copy)._"
  [^SVNRepository repo revision ^ISVNGNUDiffGenerator generator]
  (let [cli (doto (svnlook-client) (.setDiffGenerator generator))]
    (.doGetDiff cli (repo-dir repo) (core/svn-revision revision) true true true null-stream))
  generator)

(defn diff-options
  [ignore-whitespace? ignore-amount-whitespace? ignore-eol-style?]
  (SVNDiffOptions. ignore-whitespace? ignore-amount-whitespace? ignore-eol-style?))

(defn structured-generator
  ([]
     (structured-generator false))
  ([ignore-whitespace?]
     (doto (StructuredDiffGenerator.)
       (.setEncoding "ISO-8859-1")
       (.setDiffOptions (diff-options ignore-whitespace? false false)))))

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
