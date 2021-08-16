package gitlet;

import java.io.Serializable;
import java.util.TreeMap;

/**
 *  @author Nameera Faisal Akhtar
 */

public class StagingArea implements Serializable {

    /** HashMap<FileName, sha1> representing files that are staged for addition. */
    private TreeMap<String, String> stagedForAddition;

    /** HashMap<FileName, sha1> representing files that are staged for addition. */
    private TreeMap<String, String> stagedForRemoval;

    /** No-argument constructor that creates the Staging Area. */
    public StagingArea() {
        stagedForAddition = new TreeMap<>();
        stagedForRemoval = new TreeMap<>();
    }

    /** Clears all added and removed files. */
    public void clear() {
        stagedForAddition.clear();
        stagedForRemoval.clear();
    }

    /** Stages the file for addition. */
    public void stageForAddition(String fileName, String sha1) {
        stagedForAddition.put(fileName, sha1);
    }

    /** Stages the file for removal. */
    public void stageForRemoval(String fileName, String sha1) {
        stagedForRemoval.put(fileName, sha1);
    }


    /** Remove from files that are staged for removal. */
    public void removeFromStagedForRemoval(String fileName) {
        stagedForRemoval.remove(fileName);
    }

    /** Remove from files that are staged for addition. */
    public void removeFromStagedForAddition(String fileName) {
        stagedForAddition.remove(fileName);
    }

    /** Returns a HashMap of files staged for addition. */
    public TreeMap<String, String> accessAddedFiles() {
        return stagedForAddition;
    }

    /** Returns a HashMap of files staged for removal. */
    public TreeMap<String, String> accessRemovedFiles() {
        return stagedForRemoval;
    }

}
