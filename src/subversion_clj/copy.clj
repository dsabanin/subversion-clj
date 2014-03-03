(ns subversion-clj.copy
  (:require
    [subversion-clj.core :as core])
  (:use
    subversion-clj.utils)
  (:import
    [org.tmatesoft.svn.core SVNURL SVNException SVNProperties]
    [org.tmatesoft.svn.core.auth ISVNAuthenticationManager]
    [org.tmatesoft.svn.core.wc SVNCopyClient SVNWCUtil SVNCopySource ISVNOptions SVNRevision]))

(defn default-options
  "Return a new ISVNOptions instance with default options"
  []
  (SVNWCUtil/createDefaultOptions true))

(defn copy-client
  "Returns a new SVNCopyClient instance"
  [username password]
  (new SVNCopyClient (core/auth-manager username password) (default-options)))

(defn copy-source-from-url
  "Returns a new SVNCopySource instance"
  [url]
  (new SVNCopySource SVNRevision/HEAD, SVNRevision/HEAD, (core/svn-url url)))

; copy
; Parameters:
;   username (String) - Required when copying over remote. Optional for local repository.
;   password (String) - Required when copying over remote. Optional for local repository.
;   source (String) - What to directory to copy
;   dest (String) - Where to copy source to
;   message (String) - Commit message
;
(defn copy
  "Copy a file or directory in a working copy or in the repository."
  ([username source dest message]
    (copy username nil source dest message))
  ([username password source dest message]
    (let [sources (into-array SVNCopySource [(copy-source-from-url source)])
          dest-url (core/svn-url dest)]
      (.doCopy (copy-client username password) sources dest-url, false, false, false, message, (SVNProperties.)))))
