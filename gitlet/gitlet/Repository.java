package gitlet;

import java.io.File;
import java.util.*;

/** Represents a gitlet repository.
 * The structure is as follows:
 *  .gitlet/ -- top level folder for all persistent data in your gitlet folder
 *      - GITLET_DIR/ -- folder containing all of the persistent data
 *          - COMMITS_FOLDER -- folder containing all the commit files
 *          - STAGING_FOLDER -- folder containing the staging area
 *          - BLOBS_FOLDER -- folder containing all the blobs
 *          - BRANCHES_FOLDER -- folder containing the branches
 *
 *  @author Nameera Faisal Akhtar
 */
public class Repository {

    /**
     * The current working directory.
     */
    public static final File CWD = new File(System.getProperty("user.dir"));

    /**
     * The .gitlet directory.
     */
    public static final File GITLET_DIR = Utils.join(CWD, ".gitlet");

    /**
     * The commits folder.
     */
    public static final File COMMITS_FOLDER = Utils.join(GITLET_DIR, "commits");

    /**
     * The staging area folder
     */
    public static final File STAGING_FOLDER = Utils.join(GITLET_DIR, "staging");

    /**
     * The blob folder.
     */
    public static final File BLOBS_FOLDER = Utils.join(GITLET_DIR, "blobs");

    /**
     * The branches folder.
     */
    public static final File BRANCHES_FOLDER = Utils.join(GITLET_DIR, "branches");

    /**
     * The current branch folder.
     */
    public static final File CURRBRANCH_FOLDER = Utils.join(GITLET_DIR, "currBranch");

    /**
     * The current branch.
     */
    static String currentBranchName;

    /**
     * The head pointer points to the latest commit.
     */
    static Commit head;

    /**
     * A staging area.
     */
    static StagingArea ourStage;


    public static void init() {

        if (GITLET_DIR.isDirectory()) {
            System.out.println("A Gitlet version-control system "
                    + "already exists in the current directory.");
            System.exit(0);
        } else {
            GITLET_DIR.mkdir();
            COMMITS_FOLDER.mkdir();
            STAGING_FOLDER.mkdir();
            BLOBS_FOLDER.mkdir();
            BRANCHES_FOLDER.mkdir();
            CURRBRANCH_FOLDER.mkdir();

            // Create and save the initial commit in a file.
            // Adjust the head pointer to point to this commit.
            Commit initialCommitObject = new Commit();
            String initialSha1Id = initialCommitObject.accessId();
            File initialCommitFile = Utils.join(COMMITS_FOLDER, initialSha1Id);
            Utils.writeObject(initialCommitFile, initialCommitObject);
            head = initialCommitObject;
            saveHead();

            // Create a file containing master and
            // put the sha1 id the of the initial commit in the file.
            File masterBranch = Utils.join(BRANCHES_FOLDER, "master");
            Utils.writeContents(masterBranch, initialSha1Id);

            // Create a file containing the current branch and
            // put the name of the current branch in the file.
            currentBranchName = "master";
            File currentBranch = Utils.join(CURRBRANCH_FOLDER, "currBranch");
            Utils.writeContents(currentBranch, currentBranchName);

            // Create and save the Staging Area in a file.
            ourStage = new StagingArea();
            saveStage();
            saveCurrBranchName();
        }
    }

    public static void add(String fileName) {

        // Reading in the staging area and the head.
        ourStage = readStage();
        head = readHead();

        // If a file with the given name does not exist, print out an error message.
        if (!Utils.join(CWD, fileName).exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }

        File needToAdd = Utils.join(CWD, fileName);
        String sha1OfContents = Utils.sha1(Utils.readContents(needToAdd));

        // If the current working version of file has the same sha1 id
        // as the file passed in, they are identical.
        // In that case, do not stage it to be added and
        // remove it from the staging area if it is there.
        // Update the staging area in our file.
        if (head.accessBlob().containsKey(fileName)) {
            if (head.accessBlob().get(fileName).equals(sha1OfContents)) {
                if (ourStage.accessRemovedFiles().containsKey(fileName)) {
                    ourStage.removeFromStagedForRemoval(fileName);
                    saveStage();
                }
                if (ourStage.accessAddedFiles().containsKey(fileName)) {
                    ourStage.removeFromStagedForAddition(fileName);
                    saveStage();
                }
                System.exit(0);
            }
        }

        ourStage.stageForAddition(fileName, sha1OfContents);
        saveStage();
        saveHead();
        File blobFile = Utils.join(BLOBS_FOLDER, sha1OfContents);
        Utils.writeContents(blobFile, Utils.readContents(needToAdd));
    }

    public static void commit(String message) {

        ourStage = readStage();
        head = readHead();
        currentBranchName = readCurrBranchName();


        // If no files have been staged, print out an error message.
        if (ourStage.accessRemovedFiles().isEmpty()
                && ourStage.accessAddedFiles().isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
            // If the commit has a blank message, print out an error message.
        } else if (message.equals("")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        } else {
            // Create a new blob that has the blobs of the current commit...
            // plus the blobs of the files staged for addition...
            // minus the blobs of the files staged for removal.
            TreeMap<String, String> blobsOriginal = head.accessBlob();
            TreeMap<String, String> blobsNew = (TreeMap<String, String>) blobsOriginal.clone();

            for (String fileToRemove : ourStage.accessRemovedFiles().keySet()) {
                blobsNew.remove(fileToRemove, ourStage.accessRemovedFiles().get(fileToRemove));
            }
            for (String fileToAdd : ourStage.accessAddedFiles().keySet()) {
                blobsNew.put(fileToAdd, ourStage.accessAddedFiles().get(fileToAdd));
            }

            // Create a new commit with these blobs, save it, and adjust the head pointer.
            Commit newCommit = new Commit(message, head.accessId(), null, blobsNew);
            saveCommit(newCommit);

            head = newCommit;
            saveHead();
            saveCurrBranchName();
            updateCurrBranch();

            // Update the stage.
            ourStage.clear();
            saveStage();
        }

    }

    public static void rm(String fileName) {

        // Reading in the staging area and the head.
        ourStage = readStage();
        head = readHead();

        // Create booleans for if the file is tracked in the current commit,
        // and if it is staged for addition.
        boolean isStagedForAddition = ourStage.accessAddedFiles().containsKey(fileName);
        boolean isTracked = head.accessBlob().containsKey(fileName);
        boolean isStagedForRemoval = ourStage.accessRemovedFiles().containsKey(fileName);

        // If the file is neither staged nor tracked by the head commit, print out an error message.
        if (!isStagedForAddition && !isStagedForRemoval && !isTracked) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        } else {
            // Unstage the file if it is currently staged for addition.
            if (isStagedForAddition) {
                ourStage.removeFromStagedForAddition(fileName);
                saveStage();
            }

            // If the file is tracked in the current commit, stage it for removal
            // remove the file from the working directory if the user has not already done so.
            // Also, unstage the file if it is currently staged for addition.
            if (isTracked) {
                if (!ourStage.accessRemovedFiles().containsKey(fileName)) {
                    ourStage.stageForRemoval(fileName, head.accessBlob().get(fileName));
                }
                Utils.restrictedDelete(fileName);
                saveStage();
            }
        }
    }

    public static void log() {

        head = readHead();

        Commit pointer = head;
        while (pointer != null) {
            System.out.println("===");
            System.out.println("commit " + pointer.accessId());
            if (pointer.accessParent1() != null && pointer.accessParent2() != null) {
                System.out.println("Merge: " + shortenedId(pointer.accessParent1())
                        + " " + shortenedId(pointer.accessParent2()));
            }
            System.out.println("Date: " + pointer.accessTimestamp().toString());
            System.out.println(pointer.accessMessage());
            System.out.println();

            pointer = commitFromId(pointer.accessParent1());
        }
    }


    public static void globalLog() {

        head = readHead();
        ourStage = readStage();

        for (String fileName : Utils.plainFilenamesIn(COMMITS_FOLDER)) {

            if (!fileName.equals("headFile")) {

                Commit pointer = readCommit(fileName);

                while (pointer != null) {
                    System.out.println("===");
                    System.out.println("commit " + pointer.accessId());
                    if (pointer.accessParent1() != null && pointer.accessParent2() != null) {
                        System.out.println("Merge: " + shortenedId(pointer.accessParent1())
                                + " " + shortenedId(pointer.accessParent2()));
                    }
                    System.out.println("Date: " + pointer.accessTimestamp().toString());
                    System.out.println(pointer.accessMessage());
                    System.out.println();

                    pointer = commitFromId(pointer.accessParent1());
                }
            }
        }
    }

    public static void find(String commitMsg) {

        //  Print out the ids of all commits in COMMITS_FOLDER that have the given commit message
        int numCommitsWithMsg = 0;
        for (String fileName : Utils.plainFilenamesIn(COMMITS_FOLDER)) {
            Commit commit = readCommit(fileName);
            String msg = commit.accessMessage();
            if (msg.equals(commitMsg)) {
                if (!fileName.equals("headFile")) {
                    System.out.println(fileName);
                    numCommitsWithMsg += 1;
                }
            }
        }

        // If no such commit exists, print out an error message.
        if (numCommitsWithMsg == 0) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }
    }

    public static void status() {

        head = readHead();
        ourStage = readStage();
        currentBranchName = readCurrBranchName();

        // Branches
        List<String> branches = Utils.plainFilenamesIn(BRANCHES_FOLDER);
        Collections.sort(branches);

        System.out.println("=== Branches ===");
        for (String branchName : branches) {
            if (currentBranchName.equals(branchName)) {
                System.out.print("*");
            }
            System.out.println(branchName);
        }
        System.out.println();

        // Staged Files

        ArrayList<String> stagedFiles = new ArrayList<String>();
        stagedFiles.addAll(ourStage.accessAddedFiles().keySet());
        Collections.sort(stagedFiles);

        System.out.println("=== Staged Files ===");
        for (String stagedFileName : stagedFiles) {
            System.out.println(stagedFileName);
        }
        System.out.println();


        // Removed files
        ArrayList<String> removedFiles = new ArrayList<String>();
        removedFiles.addAll(ourStage.accessRemovedFiles().keySet());
        Collections.sort(removedFiles);

        System.out.println("=== Removed Files ===");
        for (String removedFileName : removedFiles) {
            System.out.println(removedFileName);
        }
        System.out.println();

        // Modifications not staged for commits
        System.out.println("=== Modifications Not Staged For Commit ===");
        Map<String, String> modifiedUnstagedFiles = findModifiedUnstagedFiles();
        for (String modifiedUnstagedFileName : modifiedUnstagedFiles.keySet()) {
            System.out.println(modifiedUnstagedFileName + "("
                    + modifiedUnstagedFiles.get(modifiedUnstagedFileName) + ")");
        }
        System.out.println();

        // Untracked files
        System.out.println("=== Untracked Files ===");
        ArrayList<String> untrackedFiles = findUntrackedFiles();
        for (String untrackedFileName : untrackedFiles) {
            System.out.println(untrackedFileName);
        }
        System.out.println();
    }

    public static void checkoutFileName(String fileName) {

        head = readHead();
        ourStage = readStage();

        checkoutCommitIdFileName(head.accessId(), fileName);
    }


    public static void checkoutCommitIdFileName(String commitId, String fileName) {

        head = readHead();
        ourStage = readStage();

        commitId = fullId(commitId);
        // If no commit with the given id exists, print out an error message.

        if (commitId.equals("")
                || !Utils.join(COMMITS_FOLDER, commitId).exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }

        Commit givenCommit = commitFromId(commitId);

        if (!givenCommit.accessBlob().containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }

        // Else...
        // takes the version of the file as it exists in the commit with the given id,
        // and puts it in the working directory
        if (Utils.join(CWD, fileName).exists()) {
            Utils.restrictedDelete(fileName);
        }
        String versionWantedId = givenCommit.accessBlob().get(fileName);
        File versionWantedFile = Utils.join(BLOBS_FOLDER, versionWantedId);
        byte[] versionWantedContents = Utils.readContents(versionWantedFile);
        File newFile = Utils.join(CWD, fileName);
        Utils.writeContents(newFile, versionWantedContents);

        saveHead();
        saveStage();
    }

    public static void checkoutBranchName(String branchName) {

        head = readHead();
        ourStage = readStage();
        currentBranchName = readCurrBranchName();

        // If no branch with that name exists, print error message
        if (!Utils.join(BRANCHES_FOLDER, branchName).exists()) {
            System.out.println("No such branch exists.");
            System.exit(0);
            // If that branch is the current branch, print error message
        } else if (branchName.equals(currentBranchName)) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }

        // If a working file is untracked in the current branch and
        // would be overwritten by the checkout, print error message
        File newBranchFile = Utils.join(BRANCHES_FOLDER, branchName);
        String newBranchSha1 = Utils.readContentsAsString(newBranchFile);
        File newBranchCommitFile = Utils.join(COMMITS_FOLDER, newBranchSha1);
        Commit newBranchCommit = Utils.readObject(newBranchCommitFile, Commit.class);

        for (String fileName : Utils.plainFilenamesIn(CWD)) {
            if (!head.accessBlob().containsKey(fileName)) {
                String sha1InCWD = Utils.sha1(Utils.readContents(Utils.join(CWD, fileName)));
                String sha1InCheckedOut = newBranchCommit.accessId();
                if (!sha1InCheckedOut.equals(sha1InCWD)) {
                    System.out.println("There is an untracked file in the way; "
                            + "delete it, or add and commit it first.");
                    System.exit(0);
                }
            }
        }

        // Any files that are tracked in the current branch
        // but are not present in the checked-out branch are deleted.
        for (String fileName : Utils.plainFilenamesIn(CWD)) {
            if (!newBranchCommit.accessBlob().containsKey(fileName)
                    && head.accessBlob().containsKey(fileName)) {
                Utils.restrictedDelete(fileName);
            }
        }

        // Takes all files in the commit at the head of the given branch,
        // and puts them in the working directory.
        // If you're currently on branch1 and there is a file f.txt,
        // and you checkout to master branch,
        // which has a different version of f.txt,
        // you should be updating your CWD with master's version
        // of f.txt.
        for (String fileName : newBranchCommit.accessBlob().keySet()) {
            String versionWantedId = newBranchCommit.accessBlob().get(fileName);
            File versionWantedFile = Utils.join(BLOBS_FOLDER, versionWantedId);
            byte[] versionWantedContents = Utils.readContents(versionWantedFile);
            File newFile = Utils.join(CWD, fileName);
            Utils.writeContents(newFile, versionWantedContents);
        }

        if (!currentBranchName.equals(branchName)) {
            ourStage.clear();
            saveStage();
        }

        head = newBranchCommit;
        saveHead();
        currentBranchName = branchName;
        saveCurrBranchName();
        updateCurrBranch();
    }

    public static void branch(String branchName) {
        head = readHead();

        // If a branch with the given name already exists, print error.
        if (Utils.join(BRANCHES_FOLDER, branchName).exists()) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }

        // Create a new branch with the given name,
        // and point it at the current head commit
        File branchFile = Utils.join(BRANCHES_FOLDER, branchName);
        Utils.writeContents(branchFile, head.accessId());

    }

    public static void rmBranch(String branchName) {
        currentBranchName = readCurrBranchName();

        // If you try to remove the branch you’re currently on, print error.
        if (currentBranchName.equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }

        // If a branch with the given name does not exist, print error.
        File branchFile = Utils.join(BRANCHES_FOLDER, branchName);
        if (!branchFile.delete()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
    }

    public static void reset(String commitId) {
        ourStage = readStage();
        head = readHead();
        currentBranchName = readCurrBranchName();

        commitId = fullId(commitId);

        if (commitId.equals("")
                || !Utils.join(COMMITS_FOLDER, commitId).exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }

        Commit givenCommit = readCommit(commitId);

        //  If a working file is untracked in the current branch
        //  and would be overwritten by the reset, print error
        for (String fileName : Utils.plainFilenamesIn(CWD)) {
            if (givenCommit.accessBlob().containsKey(fileName)
                    && !head.accessBlob().containsKey(fileName)) {
                String sha1InCWD = Utils.sha1(Utils.readContents(Utils.join(CWD, fileName)));
                String sha1InCheckedOut = givenCommit.accessId();
                if (!sha1InCheckedOut.equals(sha1InCWD)) {
                    System.out.println("There is an untracked file in the way; "
                            + "delete it, or add and commit it first.");
                    System.exit(0);
                }
            }
        }

        // Removes tracked files that are not present in given commit
        for (String fileName : Utils.plainFilenamesIn(CWD)) {
            if (!givenCommit.accessBlob().containsKey(fileName)) {
                Utils.restrictedDelete(fileName);
            }
        }

        // Checks out all the files tracked by the given commit
        for (String fileName : givenCommit.accessBlob().keySet()) {
            checkoutCommitIdFileName(commitId, fileName);
        }


        head = givenCommit;
        ourStage.clear();
        saveStage();
        saveHead();
        updateCurrBranch();
        saveCurrBranchName();

    }

    /**
     * Here are the cases for merge:
     * Case 1 : SPLIT =/= NULL, CURR =/= NULL, GIVEN =/= NULL
     * GIVEN MODIFIED, CURR NOT MODIFIED -> checkout n add
     * Case 2 : SPLIT =/= NULL, CURR =/= NULL, GIVEN =/= NULL
     * GIVEN NOT MODIFIED, CURR MODIFIED -> do nothing
     * Case 3 : SPLIT =/= NULL, CURR =/= NULL, GIVEN =/= NULL
     * GIVEN AND CURR MODIFIED TO THE SAME -> do nothing
     * Case 6 : SPLIT =/= NULL, CURR =/= NULL, GIVEN == NULL
     * CURR NOT MODIFIED -> remove
     * Case 7 : SPLIT =/= NULL, CURR == NULL, GIVEN =/= NULL
     * GIVEN NOT MODIFIED -> do nothing
     * The latter half:
     * Case 4 : SPLIT == NULL, CURR =/= NULL, GIVEN == NULL
     * do nothing!
     * Case 5 : SPLIT == NULL, CURR == NULL, GIVEN =/= NULL
     * check out and stage!
     */

    public static void merge(String branchName) {
        ourStage = readStage();
        head = readHead();
        currentBranchName = readCurrBranchName();
        checkMergeFailureCases(branchName);
        Commit givenCommit = branchCommit(branchName);
        Commit currentCommit = head;
        Commit splitPoint = findSplitPoint3(givenCommit, currentCommit, branchName);

        if (splitPoint == null) {
            return;
        }
        HashSet<String> allFileNames = new HashSet<>();
        for (String fileName : givenCommit.accessBlob().keySet()) {
            allFileNames.add(fileName);
        }
        for (String fileName : currentCommit.accessBlob().keySet()) {
            allFileNames.add(fileName);
        }
        for (String fileName : splitPoint.accessBlob().keySet()) {
            allFileNames.add(fileName);
        }
        boolean mergeConflict = false;
        for (String fileName : allFileNames) {
            String splitPointContents = splitPoint.accessBlob().get(fileName);
            String currCommitContents = currentCommit.accessBlob().get(fileName);
            String givenCommitContents = givenCommit.accessBlob().get(fileName);
            if (splitPointContents != null) {
                if (currCommitContents != null && givenCommitContents != null) {
                    if (!splitPointContents.equals(givenCommitContents)
                            && splitPointContents.equals(currCommitContents)) {
                        checkoutCommitIdFileName(givenCommit.accessId(), fileName);
                        add(fileName);
                    }
                } else if (givenCommitContents == null && currCommitContents != null) {
                    if (splitPointContents.equals(currCommitContents)) {
                        rm(fileName);
                    }
                }
            } else {
                if (currCommitContents == null && givenCommitContents != null) {
                    checkoutCommitIdFileName(givenCommit.accessId(), fileName);
                    add(fileName);
                }
            }
            if (isInConflict(givenCommit, currentCommit, splitPoint, fileName)) {
                mergeConflict = true;
                String givenContent = "";
                String currContent = "";
                String sha1OfGivenContents = givenCommit.accessBlob().get(fileName);
                String sha1OfCurrContents = currentCommit.accessBlob().get(fileName);
                if (sha1OfGivenContents != null) {
                    File givenBlobFile = Utils.join(BLOBS_FOLDER, sha1OfGivenContents);
                    if (givenBlobFile.exists()) {
                        givenContent = Utils.readContentsAsString(givenBlobFile);
                    }
                }
                if (sha1OfCurrContents != null) {
                    File currBlobFile = Utils.join(BLOBS_FOLDER, sha1OfCurrContents);
                    if (currBlobFile.exists()) {
                        currContent = Utils.readContentsAsString(currBlobFile);
                    }
                }
                String newContents = "<<<<<<< HEAD\n" + currContent + "=======\n"
                        + givenContent + ">>>>>>>\n";
                File f = Utils.join(CWD, fileName);
                Utils.writeContents(f, newContents);
                add(f.getName());
            }
        }
        String logMsg = "Merged " + branchName + " into " + currentBranchName + ".";
        commit(logMsg);
        head.setParent1(currentCommit.accessId());
        head.setParent2(givenCommit.accessId());
        if (mergeConflict) {
            System.out.println("Encountered a merge conflict.");
        }
        saveHead();
        saveStage();
    }

    /**
     * Helper methods.
     */


    private static Commit findSplitPoint3(Commit given, Commit current, String bName) {
        head = readHead();
        Commit splitPoint = null;
        Commit storeGiven = given;
        Commit storeCurr = current;

        Commit givenPointer = given;
        Queue<Commit> fringe1 = new LinkedList<>();
        fringe1.add(givenPointer);
        HashSet<String> seen1 = new HashSet<>();
        while (!fringe1.isEmpty()) {
            Commit nextCommit = fringe1.remove();
            seen1.add(nextCommit.accessId());
            for (String parentId : nextCommit.getBothParents()) {
                if (!seen1.contains(parentId)) {
                    fringe1.add(commitFromId(parentId));
                }
            }
        }

        HashSet<String> seen2 = new HashSet<>();
        Commit currentPointer = current;
        Queue<Commit> fringe2 = new LinkedList<>();
        fringe2.add(currentPointer);
        while (!fringe2.isEmpty()) {
            Commit nextCommit = fringe2.remove();
            seen2.add(nextCommit.accessId());
            for (String parentId : nextCommit.getBothParents()) {
                if (seen1.contains(parentId)) {
                    splitPoint = commitFromId(parentId);
                    break;
                }
                fringe2.add(commitFromId(parentId));
            }
        }


        ArrayList<String> historyOfGiven = new ArrayList<>();
        ArrayList<String> historyOfCurr = new ArrayList<>();
        Commit givenCommitPointer = given;
        Commit currentCommitPointer = current;
        Commit splitPoint2 = null;
        while (givenCommitPointer != null
                && givenCommitPointer.accessParent1() != null) {
            historyOfGiven.add(givenCommitPointer.accessId());
            givenCommitPointer = commitFromId(givenCommitPointer.accessParent1());
        }
        while (currentCommitPointer != null
                && currentCommitPointer.accessParent1() != null) {
            historyOfCurr.add(currentCommitPointer.accessId());
            if (historyOfGiven.contains(currentCommitPointer.accessId())) {
                splitPoint2 = currentCommitPointer;
                break;
            }
            currentCommitPointer = commitFromId(currentCommitPointer.accessParent1());
        }

        if (splitPoint2 != null) {
            if (splitPoint2.accessId().equals(current.accessId())) {
                checkoutBranchName(bName);
                System.out.println("Current branch fast-forwarded.");
                System.exit(0);
            }
            if (splitPoint2.accessId().equals(given.accessId())) {
                System.out.println("Given branch is an ancestor of the current branch.");
                System.exit(0);
            }
        }

        return splitPoint;
    }

    public static void checkMergeFailureCases(String branchName) {
        ourStage = readStage();
        currentBranchName = readCurrBranchName();
        head = readHead();
        if (!ourStage.accessAddedFiles().isEmpty()
                || !ourStage.accessRemovedFiles().isEmpty()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        if (!Utils.join(BRANCHES_FOLDER, branchName).exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (currentBranchName.equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
        File branchFile = Utils.join(BRANCHES_FOLDER, branchName);
        String commitId = Utils.readContentsAsString(branchFile);
        File commitFile = Utils.join(COMMITS_FOLDER, commitId);
        Commit commit = Utils.readObject(commitFile, Commit.class);
        for (String fileName : Utils.plainFilenamesIn(CWD)) {
            if (commit.accessBlob().containsKey(fileName)
                    && !head.accessBlob().containsKey(fileName)) {
                String sha1InCWD = Utils.sha1(Utils.readContents(Utils.join(CWD, fileName)));
                String sha1InCheckedOut = commit.accessId();
                if (!sha1InCheckedOut.equals(sha1InCWD)) {
                    System.out.println("There is an untracked file in the way; "
                            + "delete it, or add and commit it first.");
                    System.exit(0);
                }
            }
        }
    }

    /**
     * 3 ways of being in conflict:
     * 1) contents of both are changed and different from other
     * 2) the contents of one are changed and the other file is deleted
     * 3) file absent at split point and has diff contents in given & curr branches
     */
    private static boolean isInConflict(Commit given, Commit curr, Commit split, String fileName) {
        String givenId = given.accessBlob().get(fileName);
        String currId = curr.accessBlob().get(fileName);
        String splitId = split.accessBlob().get(fileName);
        if (splitId != null && currId != null && givenId != null) {
            if (!splitId.equals(givenId) && !splitId.equals(currId)) {
                if (!givenId.equals(currId)) {
                    return true;
                }
            }
        } else if (splitId != null && currId != null && givenId == null) {
            if (!splitId.equals(currId)) {
                return true;
            }
        } else if (splitId != null && currId == null && givenId != null) {
            if (!splitId.equals(givenId)) {
                return true;
            }
        } else if (splitId == null && currId != null && givenId != null) {
            if (!givenId.equals(currId)) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, String> findModifiedUnstagedFiles() {
        head = readHead();
        ourStage = readStage();

        Map<String, String> modifiedUnstagedFiles = new HashMap<String, String>();

        for (String fileName : Utils.plainFilenamesIn(CWD)) {
            boolean isInCWD = Utils.join(CWD, fileName).exists();

            if (isInCWD) {
                File f = Utils.join(CWD, fileName);
                String commitId = Utils.sha1(Utils.readContents(f));

                boolean isTrackedInCurrent = head.accessBlob().containsKey(fileName);
                boolean isStagedForAddition = ourStage.accessAddedFiles().containsKey(fileName);

                // Tracked in the current commit, changed in the working directory but not staged or
                // Staged for addition, but with different contents than in the working directory
                if (isTrackedInCurrent && !head.accessBlob().get(fileName).equals(commitId)) {
                    //if (!isStagedForAddition && !isStagedForRemoval) {
                    modifiedUnstagedFiles.put(fileName, "modified");
                    //}
                } else if (isStagedForAddition
                        && !ourStage.accessAddedFiles().get(fileName).equals(commitId)) {
                    modifiedUnstagedFiles.put(fileName, "modified");
                }
            }
        }

        // Staged for addition, but deleted in the working directory; or
        // Not staged for removal, but tracked in the current commit and
        // deleted from the working directory.

        for (String fileName : head.accessBlob().keySet()) {
            boolean isDeletedInWorking = !Utils.plainFilenamesIn(CWD).contains(fileName);
            boolean isStagedForRemoval = ourStage.accessRemovedFiles().containsKey(fileName);
            boolean isStagedForAddition = ourStage.accessAddedFiles().containsKey(fileName);
            boolean isTrackedInCurrent = head.accessBlob().containsKey(fileName);

            if (isStagedForAddition && isDeletedInWorking) {
                modifiedUnstagedFiles.put(fileName, "deleted");
            } else if (!isStagedForRemoval && isTrackedInCurrent && isDeletedInWorking) {
                modifiedUnstagedFiles.put(fileName, "deleted");
            }
        }
        return modifiedUnstagedFiles;
    }


    private static ArrayList<String> findUntrackedFiles() {
        head = readHead();
        ourStage = readStage();

        ArrayList<String> untrackedFiles = new ArrayList<String>();
        //is for files present in the working directory but neither staged for addition nor tracked.
        for (String fileName : Utils.plainFilenamesIn(CWD)) {
            boolean stagedForAddition = ourStage.accessAddedFiles().containsKey(fileName);
            boolean isTrackedInCurrent = head.accessBlob().containsKey(fileName);
            if (!stagedForAddition && !isTrackedInCurrent) {
                untrackedFiles.add(fileName);
            }
        }
        return untrackedFiles;
    }

    private static String fullId(String shortId) {
        for (String commitId : Utils.plainFilenamesIn(COMMITS_FOLDER)) {
            if (commitId.startsWith(shortId)) {
                return commitId;
            }
        }
        return "";
    }

    private static Commit branchCommit(String branchName) {
        File branchFile = Utils.join(BRANCHES_FOLDER, branchName);
        String givenCommitId = Utils.readContentsAsString(branchFile);
        File givenCommitFile = Utils.join(COMMITS_FOLDER, givenCommitId);
        Commit givenCommit = Utils.readObject(givenCommitFile, Commit.class);
        return givenCommit;
    }

    private static void saveCommit(Commit c) {
        File cFile = Utils.join(COMMITS_FOLDER, c.accessId());
        Utils.writeObject(cFile, c);
    }

    private static Commit readCommit(String commitId) {
        File cFile = Utils.join(COMMITS_FOLDER, commitId);
        return Utils.readObject(cFile, Commit.class);
    }

    private static void saveStage() {
        File sFile = Utils.join(STAGING_FOLDER, "ourStage");
        Utils.writeObject(sFile, ourStage);
    }

    private static StagingArea readStage() {
        File sFile = Utils.join(STAGING_FOLDER, "ourStage");
        return Utils.readObject(sFile, StagingArea.class);
    }

    private static void saveHead() {
        File hFile = Utils.join(COMMITS_FOLDER, "headFile");
        Utils.writeObject(hFile, head);
    }

    private static Commit readHead() {
        File hFile = Utils.join(COMMITS_FOLDER, "headFile");
        return Utils.readObject(hFile, Commit.class);
    }

    private static String readCurrBranchName() {
        File currBranchFile = Utils.join(CURRBRANCH_FOLDER, "currBranch");
        return Utils.readContentsAsString(currBranchFile);
    }

    private static void saveCurrBranchName() {
        File currBranchFile = Utils.join(CURRBRANCH_FOLDER, "currBranch");
        Utils.writeContents(currBranchFile, currentBranchName);
    }


    private static Commit commitFromId(String id) {
        if (id == null) {
            return null;
        }
        File commitFile = Utils.join(COMMITS_FOLDER, id);
        return Utils.readObject(commitFile, Commit.class);
    }

    public static void updateCurrBranch() {
        File f = Utils.join(BRANCHES_FOLDER, currentBranchName);
        Utils.writeContents(f, head.accessId());
    }

    public static byte[] serializeCommit(Commit c) {
        return Utils.serialize(c);
    }

    private static String shortenedId(String id) {
        return id.substring(0, 7);
    }

}


