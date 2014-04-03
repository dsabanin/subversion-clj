(ns subversion-clj.commit
  (:require
    [subversion-clj.core :as core])
  (:use
    subversion-clj.utils)
  (:import
    [org.tmatesoft.svn.core SVNURL SVNException SVNProperties]
    [org.tmatesoft.svn.core.auth ISVNAuthenticationManager]
    [org.tmatesoft.svn.core.wc SVNCopyClient SVNWCUtil SVNCopySource ISVNOptions SVNRevision SVNClientManager]))


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
