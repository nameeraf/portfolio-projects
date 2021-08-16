# Gitlet Design Document

**Name**: Nameera Faisal Akhtar

## Classes and Data Structures

### 1) Repository.java
Basically implements all the commands
#### Fields

- File GITLET_DIR
  - File STAGING_FOLDER 
  - File COMMITS_FOLDER
  - File BLOBS_FOLDER 
  - File BRANCHES_FOLDER
  - File CURRBRANCH_FOLDER
    
- Commit head (a Commit object which is the HEAD commit)
- StagingArea ourStage (a Staging Area object)
- String currBranchName (a string which holds the name of the current branch)

### 2) Commit.java
Basically creates a commit object, and has methods for commit objects including creating commits, getting their sha1 id and accessing instance variables.

#### Fields

1. String message - contains the message of the commit
2. Date timestamp - contains the time of the commit
3. String id - contains the sha1 hash of the commit
4. String parentId - contains the sha1 hash of the parent of the commit
5. HashMap blob - stores blobs in a <FileName, sha1> structure


### 3) StagingArea.java
Creates a StagingArea object which has a clear method which removes all files staged for addition and removal, a constructor that creates a StagingArea, and stageForAddition and stageForRemoval methods which put a file in their respective fields.

#### Fields
1. HashMap stagedForRemoval - files staged for removal in a <FileName, sha1> structure
2. HashMap stagedforAddition - files staged for addition in a <FileName, sha1> structure

## Algorithms

### 1) Repository 

On a high level, this class contains all the commands that we are required to implement in Gitlet.
It contains readHead(), saveHead(), readStage() and saveStage() helper methods that help set up persistence.

### 2) StagingArea
- Constructor: Creates a new StagingArea object by instantiating 2 new Hashmaps: stagedForAddition and stagedForRemoval
- clear(): Empties the adding staging and removal stage
- stageForRemoval(String fileName, String sha1) : adds file with fileName and sha1 to the stage for removal
- stageForAddition(String fileName, String sha1) : adds file with fileName and sha1 to the stage for addition
- removeFromStagedForRemoval(String fileName) : removes file with fileName from the removal staging area
- removeFromStagedForAddition(String fileName) : removes file with fileName from the addition staging area
- All other methods : all other methods are "access" methods i.e. so we can access StagingArea instance variables in the Repository class.

### 3) Commit
- initial commit constructor: makes the initial commit and sets up instance variables for the initial commit
- normal commit constructors: makes all other constructors after the initial commit and sets up instance variables for this commit
- All other methods : all other methods are "access" methods i.e. so we can access Commit instance variables in the Repository class.

## Persistence
Most of the persistence is happening in Repository.java.
Thus, Repository.java creates the following file structure:
- File GITLET_DIR
    - File STAGING_FOLDER
      - This contains one File called ourStage which stores the StagingArea object.
    - File COMMITS_FOLDER
      - This contains a new File for each commit where the fileName is the sha1 id of the commit.
    - File BLOBS_FOLDER
      - This contains a new File for each new version of the file where the fileName is the sha1 id of the contents.
    - File BRANCHES_FOLDER
      - This contains a new File for each branch where the name of the file is the name of the branch.
    - File CURRBRANCH_FOLDER
      - This contains one File called currBranch which stores the name of the current branch.
    

My class contains readHead(), saveHead(), readStage() and saveStage() helper methods that help set up persistence.
I also added a readCurrBranch() and saveCurrBranch() methods.


