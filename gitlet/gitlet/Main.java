package gitlet;

import java.io.File;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Nameera Faisal Akhtar
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
        }

        else {
            String firstArg = args[0];
            switch(firstArg) {
                case "init":
                    if (args.length == 1) {
                        Repository.init();
                    } else {
                        System.out.println("Incorrect operands.");
                    }
                    break;
                case "add":
                    if (args.length == 2) {
                        Repository.add(args[1]);
                    } else {
                        System.out.println("Incorrect operands.");
                    }
                    break;
                case "commit":
                    if (args.length == 2) {
                        Repository.commit(args[1]);
                    } else {
                        System.out.println("Incorrect operands.");
                    }
                    break;
                case "rm":
                    if (args.length == 2) {
                        Repository.rm(args[1]);
                    } else {
                        System.out.println("Incorrect operands.");
                    }
                    break;
                case "log":
                    if (args.length == 1) {
                        Repository.log();
                    } else {
                        System.out.println("Incorrect operands.");
                    }
                    break;
                case "global-log":
                    if (args.length == 1) {
                        Repository.globalLog();
                    } else {
                        System.out.println("Incorrect operands.");
                    }
                    break;
                case "find":
                    if (args.length == 2) {
                        Repository.find(args[1]);
                    } else {
                        System.out.println("Incorrect operands.");
                    }
                    break;
                case "status":
                    checkForExistence();
                    if (args.length == 1) {
                        Repository.status();
                    } else {
                        System.out.println("Incorrect operands.");
                    }
                    break;
                case "checkout":
                    if (args.length == 3 && args[1].equals("--")) {
                        Repository.checkoutFileName(args[2]);
                    }
                    else if (args.length == 4 && args[2].equals("--")) {
                        Repository.checkoutCommitIdFileName(args[1], args[3]);
                    }
                    else if (args.length == 2) {
                        Repository.checkoutBranchName(args[1]);
                    }
                    else {
                        System.out.println("Incorrect operands.");
                    }
                    break;
                case "branch":
                    if (args.length == 2) {
                        Repository.branch(args[1]);
                    } else {
                        System.out.println("Incorrect operands.");
                    }
                    break;
                case "rm-branch":
                    if (args.length == 2) {
                        Repository.rmBranch(args[1]);
                    } else {
                        System.out.println("Incorrect operands.");
                    }
                    break;
                case "reset":
                    if (args.length == 2) {
                        Repository.reset(args[1]);
                    } else {
                        System.out.println("Incorrect operands.");
                    }
                    break;
                case "merge":
                    if (args.length == 2) {
                        Repository.merge(args[1]);
                    } else {
                        System.out.println("Incorrect operands.");
                    }
                    break;
                default:
                    System.out.println("No command with that name exists.");
            }
        }
        System.exit(0);
    }

    private static void checkForExistence() {
        if (!Repository.GITLET_DIR.isDirectory()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }
}
