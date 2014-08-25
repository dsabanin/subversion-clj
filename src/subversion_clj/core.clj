;; ## Read-only Subversion access
;;
;; This code is extracted from <a href="http://beanstalkapp.com">beanstalkapp.com</a> caching daemon[1].
;;
;; Right now this is just a read-only wrapper around Java's SVNKit that allows you to look
;; into contents of local and remote repositories (no working copy needed).
;;
;; At this moment all this library can do is get unified information about all revisions or some particular revision
;; in the repo. However I'm planning to extend this code as Beanstalk uses more Clojure code
;; for performance critical parts
;;
;; [1] <a href="http://blog.beanstalkapp.com/post/23998022427/beanstalk-clojure-love-and-20x-better-performance">Post in Beanstalk's blog about this</a>
;;

(ns subversion-clj.core
    (:require
    [clojure.string :as string]
    subversion-clj.diff-generator)
  (:use
    subversion-clj.utils)
  (:import
     [org.tmatesoft.svn.core.internal.io.fs FSRepositoryFactory FSPathChange]
     [org.tmatesoft.svn.core.internal.io.dav DAVRepositoryFactory]
     [org.tmatesoft.svn.core.internal.io.svn SVNRepositoryFactoryImpl]
     [org.tmatesoft.svn.core.internal.util SVNHashMap SVNHashMap$TableEntry]
     [org.tmatesoft.svn.core SVNURL SVNLogEntry SVNLogEntryPath SVNException SVNNodeKind]
     [org.tmatesoft.svn.core.io SVNRepository SVNRepositoryFactory]
     [org.tmatesoft.svn.core.wc SVNWCUtil SVNClientManager SVNRevision]
     [org.apache.commons.io.output NullOutputStream]
     [java.io File ByteArrayOutputStream]
     [java.util LinkedList]
     [subversion.clj StructuredDiffGenerator]
     [org.tmatesoft.svn.core.wc.admin ISVNGNUDiffGenerator SVNLookClient]))

(declare log-record log-obj)

(DAVRepositoryFactory/setup)
(SVNRepositoryFactoryImpl/setup)
(FSRepositoryFactory/setup)

(defn auth-manager
  "Creates username/password authenticated AuthenticationManager instance."
  [username password]
  (SVNWCUtil/createDefaultAuthenticationManager username password))

(defn svn-url
  "Returns new SVNURL instance."
  [uri]
  (SVNURL/parseURIEncoded uri))

(defn repo-for
  "Creates an instance of SVNRepository subclass from a legitimate Subversion URL like:

  * `https://wildbit.svn.beanstalkapp.com/somerepo`
  * `file:///storage/somerepo`
  * `svn://internal-server:3122/somerepo`

  You can use it like:

        (repo-for \"file:///storage/my-repo\")

  Or like this:

        (repo-for
          \"https://wildbit.svn.beanstalkapp.com/repo\"
          \"login\"
          \"pass\")"
  (^SVNRepository [uri]
                  (SVNRepositoryFactory/create (svn-url uri)))

  (^SVNRepository [uri username password]
                  (doto (repo-for uri)
                    (.setAuthenticationManager (auth-manager username password)))))

(defn revisions-for
  "Returns an array with all the revision records in the repository."
  ([^SVNRepository repo]
     (revisions-for repo (string-array) 1 -1))
  ([^SVNRepository repo paths from-rev to-rev]
     (->> (.log repo paths (linked-list) from-rev to-rev true false)
          (map #(log-record %))
          (into []))))


(defn revision-for
  "Returns an individual revision record.

   Example record for a copied directory:

        {:revision 6
        :author \"railsmonk\"
        :message \"copied directory\"
        :changes [[\"dir\" [\"new-dir\" \"old-dir\" 5] :copy]]}

   Example record for an edited files:

        {:revision 11
        :author \"railsmonk\"
        :message \"editing files\"
        :changes [[\"file\" \"commit1\" :edit]
                  [\"file\" \"commit3\" :edit]]}"
  [^SVNRepository repo ^Long revision]
  (let [revision (Long. revision)]
    (->> (.log repo (string-array) (linked-list) revision revision true false)
      first
      log-record)))

(defn- basename
  [path]
  (.getName (File. ^String path)))

(defn- node-kind-at-rev ^String
  [^SVNRepository repo ^String path ^Long rev]
  (.. (.checkPath repo path rev) toString))

(defn node-kind
  "Returns kind of a node path at certain revision - file or directory."
  [repo path rev]
  (let [node-kind-at-current-rev (node-kind-at-rev repo path rev)]
    (if (= "none" node-kind-at-current-rev)
      (node-kind-at-rev repo path (dec rev))
      node-kind-at-current-rev)))

(def letter->change-sym
  {\A :add
   \M :edit
   \D :delete
   \R :replace})

(defn- change-kind
  [^SVNLogEntryPath change-rec]
  (let [change-letter (.getType change-rec)
        copy-rev (.getCopyRevision change-rec)]
    (if (neg? copy-rev)
      (letter->change-sym change-letter)
      :copy)))

(defn- detailed-path
  [^SVNHashMap$TableEntry [^String path ^SVNLogEntryPath node]]
  (let [path (normalize-path path)
        node-kind (str ^SVNNodeKind (.getKind node))
        change-kind (change-kind node)]
    (if (= :copy change-kind)
      [node-kind [path (normalize-path (.getCopyPath node)) (.getCopyRevision node)] change-kind]
      [node-kind path change-kind])))

(defn- changed-paths
  [^SVNLogEntry log-obj]
  (let [changes (.getChangedPaths log-obj)]
    (doall (map detailed-path changes))))

(defn- log-record [^SVNLogEntry log-obj]
  (let [revision (.getRevision log-obj)
        message (.getMessage log-obj)
        paths (changed-paths log-obj)]
    {:revision revision
     :author (.getAuthor log-obj)
     :time (.getDate log-obj)
     :message (if message (string/trim message) "")
     :changes paths}))

(defn svn-revision
  "SVNRevision instance for a given revision number."
  ^SVNRevision [revision]
  (SVNRevision/create (long revision)))

(defn youngest
  "Youngest revision of a repository."
  ^Long [^SVNRepository repo]
  (.getLatestRevision repo))

(defn uuid
  [^SVNRepository repo]
  (.getRepositoryUUID repo))

(defn client-manager
  "New SVNClientManager instance. Optional arguments are username and password for authenticated connections."
  ([]
     (client-manager nil))
  ([username password]
     (client-manager (auth-manager username password)))
  ([auth-mgr]
     (SVNClientManager/newInstance (SVNWCUtil/createDefaultOptions true)
                                   auth-mgr)))

(declare ^:dynamic *client-manager*)

(defmacro with-client-manager
  [& body]
  (let [with-args? (vector? (first body))
        args (if with-args? (first body) nil)
        body (if with-args? (rest body) body)]
    `(binding [*client-manager* (apply client-manager ~args)]
       (try
         ~@body
         (finally
           (.dispose *client-manager*))))))
