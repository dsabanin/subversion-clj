(ns subversion-clj.remote
  (:require
    [subversion-clj.core :as core])
  (:use
    subversion-clj.utils)
  (:import
    [org.tmatesoft.svn.core SVNURL SVNException SVNProperties]
    [org.tmatesoft.svn.core.auth ISVNAuthenticationManager]
    [org.tmatesoft.svn.core.wc SVNWCUtil SVNCopySource SVNRevision SVNClientManager]))

(defn copy-source-from-url
  "Takes a URL string and returns a new SVNCopySource instance"
  [url]
  (new SVNCopySource SVNRevision/HEAD, SVNRevision/HEAD, (core/svn-url url)))

(defn copy-url
  "Copies a file or directory in a given repository url or working copy.

  Useful for creating branches that will retain history for source.

  Parameters:
   SVNClientManager client: (optional)
   String source: (required) any valid Subversion URL such as \"http://userInfo@host:port/path\" or \"file:///path\"
   String destination: (required) any valid Subversion URL such as \"http://userInfo@host:port/path\" or \"file:///path\"
   String message: (required) commit message for the copy commit

  Returns:
   SVNCommitInfo object with related commit information

  Example Usage:

    (with-client-manager
      (copy-url client \"file:///repository-path/trunk\" \"file:///repository-path/branches/my-branch\" \"Created my-branch from trunk\"))

  or

    (def client (core/client-manager \"username\" \"password\"))
    (copy-url client \"file:///repository-path/trunk\" \"file:///repository-path/branches/my-branch\" \"Created my-branch from trunk\")"

  ([^String source ^String destination ^String message]
    (copy-url core/*client-manager* source destination message))
  ([^SVNClientManager client ^String source ^String destination ^String message]
    (let [sources (into-array SVNCopySource [(copy-source-from-url source)])
          destination-url (core/svn-url destination)
          props (new SVNProperties)]
      (.doCopy (.getCopyClient client) sources destination-url false false false message props))))

(defn delete-url
  "Deletes a file or directory in a given repository url or working copy.

  Parameters:
   SVNClientManager client: (optional)
   String source: (required) any valid Subversion URL such as \"http://userInfo@host:port/path\" or \"file:///path\"
   String message: (required) commit message for the commit

  Returns:
   SVNCommitInfo object with related commit information

  Example Usage:

    (with-client-manager
      (delete-url \"file:///repository-path/branches/my-stale-branch\" \"Deleted my-stale-branch\"))

  or

    (def client (core/client-manager \"username\" \"password\"))
    (delete-url client \"file:///repository-path/branches/my-stale-branch\" \"Deleted my-stale-branch\")"

  ([repo-path commit-message]
    (delete-url core/*client-manager* repo-path commit-message))
  ([^SVNClientManager client ^String repo-path ^String commit-message]
    (let [cli (.getCommitClient client)]
      (.doDelete cli (into-array SVNURL [(core/svn-url repo-path)]) commit-message))))
