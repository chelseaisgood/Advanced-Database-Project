package edu.nyu.csciga2434.project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: Minda Fang
 * Date: 9/30/16
 * Time: 6:01 PM
 * 
 * A single transaction manager that translates read and write requests on variables to read and write requests on copies using the available copy algorithm. 
 * This transaction manager never fails.
 * If the TM requests  a read for transaction T and cannot get it due to failure, the TM should try another site. 
 * If no relevant site is available, then T must wait. 
 * This applies to read-only transactions as well which must have access to the latest version of each variable before the transaction begins. 
 * T may also have to wait for conflicting locks. 
 * While T is locked, no transaction can bypass it.
 */

public class TransactionManager {

    private static final int DEFAULT_SITE_TOTAL_NUMBER = 10;

    private Map<Integer, Site> sites;
    private Map<Integer, Transaction> currentTransactions;
    private int time;
    public Map<Integer, ArrayList<Operation>> waitList;

    public TransactionManager() {
        this.sites = new HashMap<>();
        this.time = 0;
        for (int i = 1; i <= DEFAULT_SITE_TOTAL_NUMBER; i++) {
            sites.put(i, new Site(i));
        }

        currentTransactions = new HashMap<>();
        Map<Integer, ArrayList<Operation>> waitList = new HashMap<>();

    }

    public void readCommand(String commandLine) {
        //TODO
        this.time++;
        String[] operations = commandLine.split(";");
        ArrayList<Integer> endTransactionList = new ArrayList<>();
        for (String op : operations) {
            System.out.println(op);
            if (op.startsWith("begin(")) {
                begin(Integer.parseInt(op.substring(7, op.length() - 1)), TypeOfTransaction.Read_Write);
            } else if (op.startsWith("beginRO(")) {
                begin(Integer.parseInt(op.substring(9, op.length() - 1)), TypeOfTransaction.Read_Only);
            } else if (op.startsWith("R(")) {
                String[] t = op.substring(2, op.length() - 1).split(",");
                readVariableValue(Integer.parseInt(t[0].substring(1)), Integer.parseInt(t[1].substring(t[1].indexOf("x") + 1)));
            } else if (op.startsWith("W(")) {
                String[] t = op.substring(2, op.length() - 1).split(",");
                writeVariableValue(Integer.parseInt(t[0].substring(1)), Integer.parseInt(t[1].substring(t[1].indexOf("x") + 1)), Integer.parseInt(t[2]));
            } else if (op.startsWith("dump()")) {
                dump();
            } else if (op.startsWith("dump(x")) {
                int index = Integer.parseInt(op.substring(6, op.length() - 1));
                dumpVariable(index);
            } else if (op.startsWith("dump(")) {
                int index = Integer.parseInt(op.substring(5, op.length() - 1));
                dump(index);
            } else if (op.startsWith("end(")) {
                endTransactionList.add(Integer.parseInt(op.substring(4, op.length() - 1)));
            } else if (op.startsWith("fail(")) {
                failSite(Integer.parseInt(op.substring(5, op.length() - 1)));
            } else if (op.startsWith("recover(")) {
                recoverSite(Integer.parseInt(op.substring(8, op.length() - 1)));
            }
        }
        for (int i = 0; i < endTransactionList.size(); i++) {
            endTransaction(endTransactionList.get(i));
        }
    }


    private void begin(int transactionID, TypeOfTransaction typeOfTransaction) {
        if (!currentTransactions.containsKey(transactionID)) {
            Transaction newTransaction = new Transaction(transactionID, typeOfTransaction, time);
            currentTransactions.put(transactionID, newTransaction);
            if (typeOfTransaction == TypeOfTransaction.Read_Only) {
                System.out.println("[Success] Read-only transaction T" + transactionID + " has been successfully initiated..");
            } else {
                System.out.println("[Success] Transaction T" + transactionID + " has been successfully initiated..");
            }
        } else {
            System.out.println("[Failure] Transaction T" + transactionID + " might be already in progress.");
        }
    }


    private void endTransaction(Integer transactionID) {
        if (this.currentTransactions.containsKey(transactionID)) {
            Transaction transactionToBeEnded = this.currentTransactions.get(transactionID);
            if (transactionToBeEnded.getTransactionType() == TypeOfTransaction.Read_Only) {
                System.out.println("Read-only transaction T" + transactionID + " has been ended.");
            } else if (transactionToBeEnded.getTransactionType() == TypeOfTransaction.Read_Only) {
                //TODO
            }
        } else {
            System.out.println("No such transaction T" + transactionID + " to end!");
        }
    }


    private void failSite(int siteID) {
        if (this.sites.containsKey(siteID)) {
            Site s = sites.get(siteID);
            s.fail();
        } else {
            System.out.println("SUCH SITE DOES NOT EXIST (INVALID OPERATION fail(" + Integer.toString(siteID) + ")");
        }
    }

    private void recoverSite(int siteID) {
    }

    private void readVariableValue(int transactionID, int variable) {
        if (currentTransactions.containsKey(transactionID) && variable >= 1 && variable <= 20) {
            Transaction t = currentTransactions.get(transactionID);
            if (t.getTransactionType() == TypeOfTransaction.Read_Only) {
                read(transactionID, variable, TypeOfTransaction.Read_Only);
            } else {
                read(transactionID, variable, TypeOfTransaction.Read_Write);
            }
        } else {
            System.out.println("[Failure] Please check if such transaction T" + transactionID + " has began or such variable x" + variable + " exists.");
        }
    }


    private void read(int transactionID, int variableID, TypeOfTransaction typeOfTransaction) {
        Transaction transaction = currentTransactions.get(transactionID);
        if (typeOfTransaction == TypeOfTransaction.Read_Only) {
            boolean alreadyRead = false;
            for (int i = 1; i <= DEFAULT_SITE_TOTAL_NUMBER && !alreadyRead; i++) {
                Site tempSite = sites.get(i);
                if (tempSite.getIfSiteWorking()) {
                    List<Variable> variablesInThisSite = tempSite.getALLVariables();
                    for (Variable var : variablesInThisSite) {
                        if (var.getID() == variableID && var.isAvailableForReading()) {
                            //parse the VariableHistory of this Variable and select the correct value to read
                            List<VariableHistory> variableHistory = var.getVariableHistoryList();
                            int currentMax = Integer.MIN_VALUE; //time counter
                            int maxIndex = -1;  //index in the List which has max
                            for (int j = 0; j < variableHistory.size(); j++) {
                                VariableHistory vHistoryTemp = variableHistory.get(j);
                                // For read only
                                if (vHistoryTemp.getTime() > currentMax && vHistoryTemp.getTime() < transaction.getStartTime()) {
                                    maxIndex = j;
                                    currentMax = vHistoryTemp.getTime();
                                }
                            }
                            if (maxIndex != -1) {
                                alreadyRead = true;
                                int readValue = variableHistory.get(maxIndex).getValue();
                                System.out.println("[Success] The value read of variable x" + variableID + " is " + readValue + ".");
                                Operation op = new Operation(readValue, variableID, time, TypeOfOperation.OP_READ);
                                // TODO
                                transaction.addToOperationHistory(op);
                                // put successful operation into operationHistory record stored inside every transaction
                            }
                        }
                    }
                    //TODO
                }
            }
            if (!alreadyRead) {
                // The value of this variable could not be read from any up site. So this operation has to wait.
                insertToWaitList(new Operation(0, variableID, time, TypeOfOperation.OP_READ), transactionID);
                System.out.println("[Failure] Your required variable x" + variableID + " is not available at this time. Please wait!");
            }
        } else {
            // read-write
            if (ifTransactionHoldReadLockOnVariable(transaction, variableID)) {
                int index = -1;
                for (int q = 1; q <= DEFAULT_SITE_TOTAL_NUMBER; q++) {
                    if (!this.sites.get(q).getIfSiteWorking()) {
                        if (this.sites.get(q).getLockTableOfSite().
                                ifTransactionHasReadLockOnVariableInThisTable(variableID, transactionID, TypeOfLock.Read)) {
                            index = q;
                        }
                    }
                }
                List<Variable> varsInSite = this.sites.get(index).getALLVariables();
                for (Variable temp : varsInSite) {
                    if (temp.getID() == variableID) {
                        System.out.println("[Success] The value of variable x" + variableID + " that is read is " + temp.getValue() + ".");
                    }
                }
            } else {
                //TODO
            }
        }
    }

    private boolean ifTransactionHoldReadLockOnVariable(Transaction t, int variableID) {
        List<LockOnVariable> transactionLockList = t.getLocksList();
        for (int i = 0; i < transactionLockList.size(); i++) {
            if (transactionLockList.get(i).getVariableID() == variableID
                    && (transactionLockList.get(i).getLockType() == TypeOfLock.Read || transactionLockList.get(i).getLockType() == TypeOfLock.Write)) {
                return true;
            }
        }
        return false;
    }

    private void insertToWaitList(Operation op, int transactionID) {
        ArrayList<Operation> ops;
        if (waitList.containsKey(transactionID)) {
            ops = waitList.get(transactionID);
            ops.add(op);
        } else {
            ops = new ArrayList<Operation>();
            ops.add(op);
            waitList.put(transactionID, ops);
        }
    }


    private void writeVariableValue(int transactionID, int variable, int value) {
        //TODO
    }


    private void dump() {
        for (int i = 1; i <= DEFAULT_SITE_TOTAL_NUMBER; i++) {
            System.out.println("At site " + i + ":");
            System.out.println(sites.get(i).dumpOutput());//
        }
    }

    private void dump(int index) {
        if (sites.containsKey(index)) {
            System.out.println("At site " + index + ":");
            System.out.println(sites.get(index).dumpOutput());
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    private void dumpVariable(int index) {
        for (int i = 1; i <= DEFAULT_SITE_TOTAL_NUMBER; i++) {
            ArrayList<Variable> variableList = (ArrayList<Variable>) sites.get(i).getVariableList();
            for (int j = 0; j < variableList.size(); j++) {
                if (variableList.get(j).getID() == index) {
                    System.out.println("At site " + i + ":");
                    System.out.println(variableList.get(j).variableOutput());
                }
            }
        }
    }


}