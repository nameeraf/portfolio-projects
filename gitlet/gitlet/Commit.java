package gitlet;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.TreeMap;


/** Represents a gitlet commit object.
 *  @author Nameera Faisal Akhtar
 */
public class Commit implements Serializable {
    /**
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private String message;

    /** The timestamp for this Commit. */
    private String timestamp;

    /** The Sha1-hash for the parent 1 of this Commit. */
    private String parent1;

    /** The Sha1-hash for the parent 2 of this Commit. */
    private String parent2;

    /** The Sha1-hash for this Commit. */
    private String id;

    /** The blob for this Commit, in a <FileName, Sha1> structure. */
    private TreeMap<String, String> blob;


    /** Makes the initial Commit. */
    public Commit() {
        this.message = "initial commit";

        String pattern = "EEE MMM d HH:mm:ss yyyy Z";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        String date = simpleDateFormat.format(new Date(0));
        this.timestamp = date;
        this.parent1 = null;
        this.parent2 = null;
        this.blob = new TreeMap<>();
        id = Utils.sha1(Repository.serializeCommit(this));
    }

    public Commit(String msgOfCommit, String parent1Id, TreeMap<String, String> blobOfCommit) {
        String pattern = "EEE MMM d HH:mm:ss yyyy Z";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        String date = simpleDateFormat.format(new Date());
        timestamp = date;
        message = msgOfCommit;
        blob = blobOfCommit;
        parent1 = parent1Id;
        id = Utils.sha1(Repository.serializeCommit(this));
    }

    /** Makes a commit using the given message, blob and parent commits. */
    public Commit(String msgOfCommit, String parent1Id,
                  String parent2Id, TreeMap<String, String> blobOfCommit) {
        this(msgOfCommit, parent1Id, blobOfCommit);
        parent2 = parent2Id;
    }

    public String accessMessage() {
        return message;
    }

    public String accessTimestamp() {
        return timestamp;
    }

    public String accessParent1() {
        return parent1;
    }

    public String accessParent2() {
        return parent2;
    }

    public TreeMap<String, String> accessBlob()  {
        return blob;
    }

    public void setParent1(String parent1) {
        this.parent1 = parent1;
    }

    public void setParent2(String parent2) {
        this.parent2 = parent2;
    }

    public String accessId() {
        return id;
    }

    public HashSet<String> getBothParents() {
        HashSet<String> parents = new HashSet<>();
        if (this.accessParent1() != null) {
            parents.add(this.accessParent1());
        }
        if (this.accessParent2() != null) {
            parents.add(this.accessParent2());
        }
        return parents;
    }
}
