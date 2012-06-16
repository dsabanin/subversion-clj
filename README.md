# subversion-clj

## Read-only Subversion access

This code is extracted from <a href="http://beanstalkapp.com">beanstalkapp.com</a> caching daemon[1].

Right now this is just a read-only wrapper around Java's SVNKit that allows you to look
into contents of local and remote repositories (no working copy needed). 
 
At this moment all this library can do is get unified information about all revisions or some particular revision
in the repo. However I'm planning to extend this code as Beanstalk uses more Clojure code
for performance critical parts

[1] <a href="http://blog.beanstalkapp.com/post/23998022427/beanstalk-clojure-love-and-20x-better-performance">Post in Beanstalk's blog about this</a>

## Install

Available from <a href="https://clojars.org/subversion-clj">Clojars</a>.

## Usage

    ; This needs to be pointed to your repo, not working copy
    (repo-for "file:///some/path/on-your/disk/repo")

    ; Connecting to remote repository with authentication
    (repo-for "https://wildbit.svn.beanstalkapp.com/test-repo" "my-login" "my-password")

    ; Finds revision 100 in repo and returns it
    (revision-for repo 100)

    ; Finds all revisions in the repo
    (revisions-for repo)

## Example of revision records

    ; Deleted directory
    {:revision 7
    :author "dsabanin"
    :message "removed directory"
    :changes [["dir" "some-directory" :delete]]}

    ; Edited files
    {:revision 11
    :author "dsabanin"
    :message "editing files"
    :changes [["file" "file-abc" :edit]
              ["file" "file-def" :edit]]}

    ; Copied directory
    {:revision 6
    :author "dsabanin"
    :message "copied dir"
    :changes [["dir" ["new-directory" "old-directory" 5] :copy]]}

## License

Copyright (C) 2012 Dima Sabanin

Distributed under the Eclipse Public License, the same as Clojure.
