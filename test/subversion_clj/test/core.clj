(ns subversion-clj.test.core
  (:use 
    [subversion-clj.core]
    [midje.sweet])
  (:require 
    subversion-clj.diff-generator
    [subversion-clj.local :as local])
  (:import
    [org.tmatesoft.svn.core.internal.util SVNDate]
    [org.tmatesoft.svn.core.internal.io fs.FSRepository dav.DAVRepository]
    [org.tmatesoft.svn.core SVNException]
    [subversion.clj StructuredDiffGenerator]))
 
(def mock-repo-path (format "file://%s/test/test_repo_1.6" 
                            (System/getProperty "user.dir")))
 
(def records-should-be [{:revision 1 
                         :author "railsmonk"
                         :message "Added README file to an empty repo" 
                         :changes [["file" "README" :add]]}
                        {:revision 2
                         :author "railsmonk"
                         :message "added commit $i" 
                         :changes [["file" "commit1" :add]]} 
                        {:revision 3
                         :author "railsmonk" 
                         :message "added commit $i" 
                         :changes [["file" "commit2" :add]]} 
                        {:revision 4
                         :author "railsmonk" 
                         :message "added commit $i" 
                         :changes [["file" "commit3" :add]]}])

(def mock-repo (repo-for mock-repo-path))

(defn is-instance? [cls]
  (partial instance? cls))

(fact "repo-for should return mock repository"
  (repo-for mock-repo-path) => (is-instance? FSRepository))

(fact "repo-for authenticated repos"
  (repo-for "https://wildbit.svn.beanstalkapp.com/repotest" "login" "password") => (is-instance? DAVRepository))

(fact "revisions-for should return a list of revision records for a repository"
  (let [records (revisions-for mock-repo)]
    records => (is-instance? (class []))
    (first records) => (is-instance? (class {}))
    (map :revision records) => (contains [1 2 3 4])
    (map #(dissoc % :time) (take 4 records)) => (take 4 records-should-be)))

(fact "revision-for should return a revision records for a particular revision"
  (let [record (revision-for mock-repo 1)]
    record => (is-instance? (class {}))
    (dissoc record :time) => (first records-should-be)))

(fact "revision-for should raise an exception for a missing revision"
  (revision-for mock-repo 9999) => (throws SVNException))

(fact "node-kind should return proper kind"
  (let [repo mock-repo] 
    (node-kind repo "/README" 1) => "file"
    (node-kind repo "/commit3" 2) => "none"
    (node-kind repo "/commit3" 4) => "file"
    (node-kind repo "/a-dir" 5) => "dir"
    (node-kind repo "/a-dir" 2) => "none"))

(defmacro revision-fact [label rev expected]
  `(fact ~label
         (dissoc (revision-for mock-repo ~rev) :time) => ~expected))

(revision-fact "copied directory" 6
               {:revision 6
                :author "railsmonk"
                :message "copied dir"
                :changes [["dir" ["copied-dir" "a-dir" 5] :copy]]})

(revision-fact "removed directory" 7
               {:revision 7
                :author "railsmonk"
                :message "removed directory"
                :changes [["dir" "a-dir" :delete]]})

(revision-fact "file copied" 8
               {:revision 8
                :author "railsmonk"
                :message "filed copied"
                :changes [["file" ["commit-copy" "commit1" 2] :copy]]})

(revision-fact "file copied" 9
               {:revision 9
                :author "railsmonk"
                :message "updated mime-type"
                :changes [["file" "commit1" :edit]]})

(revision-fact "file removed" 10
               {:revision 10
                :author "railsmonk"
                :message "removed commit2"
                :changes [["file" "commit2" :delete]]})

(revision-fact "editing files" 11
               {:revision 11
                :author "railsmonk"
                :message "editing files"
                :changes [["file" "commit1" :edit]
                          ["file" "commit3" :edit]]})

(revision-fact "moved dir" 12
               {:revision 12
                :author "railsmonk"
                :message "moved dir"
                :changes [["dir" "copied-dir" :delete] 
                          ["dir" ["moved-dir" "copied-dir" 6] :copy]]})

(fact "diff-for should get a diff for a revision"
  (let [repo mock-repo]
    (local/diff-for repo 1) => (is-instance? java.io.ByteArrayOutputStream)
    (str (local/diff-for repo 1)) => "Added: README
===================================================================
--- /README	                        (rev 0)
+++ /README	2012-06-16 05:15:01 UTC (rev 1)
@@ -0,0 +1 @@
+This is a test repo for subversion-clj library.

"))

(fact "structured-diff-for should get a diff for a revision"
  (let [repo mock-repo
        diff (local/structured-diff-for repo 1)]
    (keys diff) => [:files :properties]
    (keys (:files diff)) => ["README"]
    (:properties diff) => {}
    (str ((:files diff) "README")) => "--- /README	                        (rev 0)
+++ /README	2012-06-16 05:15:01 UTC (rev 1)
@@ -0,0 +1 @@
+This is a test repo for subversion-clj library.

"))

(fact "structured-diff-for should get a diff for a revision and ignores whitespace"
  (let [repo mock-repo
        diff (local/structured-diff-for repo 14 true)]
    (keys diff) => [:files :properties]
    (keys (:files diff)) => ["INSTALL"]
    (:properties diff) => {}
    (str ((:files diff) "INSTALL")) => "--- /INSTALL\t2013-06-18 20:38:16 UTC (rev 13)
+++ /INSTALL\t2013-06-18 20:38:44 UTC (rev 14)
@@ -1,6 +1,4 @@
 ## INSTALL      
 
 You can install to the latest
-  
   version directly from the internet
-  
\\ No newline at end of file

"))

(fact "structured-diff-for should get a diff for a revision and doesn't ignores whitespace"
  (let [repo mock-repo
        diff (local/structured-diff-for repo 14 true)]
    (keys diff) => [:files :properties]
    (keys (:files diff)) => ["INSTALL"]
    (:properties diff) => {}
    (str ((:files diff) "INSTALL")) => "--- /INSTALL\t2013-06-18 20:38:16 UTC (rev 13)
+++ /INSTALL\t2013-06-18 20:38:44 UTC (rev 14)
@@ -1,6 +1,4 @@
 ## INSTALL      \n \n You can install to the latest
-  
   version directly from the internet
-  
\\ No newline at end of file

"))

(fact "structured-diff-for should get a diff for a revision"
  (let [repo mock-repo
        diff (local/structured-diff-for repo 1)]
    (keys diff) => [:files :properties]
    (keys (:files diff)) => ["README"]
    (:properties diff) => {}
    (str ((:files diff) "README")) => "--- /README	                        (rev 0)
+++ /README	2012-06-16 05:15:01 UTC (rev 1)
@@ -0,0 +1 @@
+This is a test repo for subversion-clj library.

"))

(fact "StructuredDiffGenerator should have list of changes"
  (let [repo mock-repo
        gen (local/diff-for! repo 11 (StructuredDiffGenerator.))]
    (.grabFileChanges gen) => {"commit1" :edit
                               "commit3" :edit}))