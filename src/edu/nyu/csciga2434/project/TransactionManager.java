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
        for (String opRaw : operations) {
            String op = opRaw.trim();
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
                writeVariableValue(Integer.parseInt(t[0].substring(1)), Integer.parseInt(t[1].substring(t[1].indexOf("x") + 1)), Integer.valueOf(t[2].trim()));
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
                }
            }
            if (!alreadyRead) {
                // The value of this variable could not be read from any up site. So this operation has to wait.
                // TODO
                insertToWaitList(new Operation(0, variableID, time, TypeOfOperation.OP_READ), transactionID);
                System.out.println("[Failure] Your required variable x" + variableID + " is not available at this time. Please wait!");
            }
        } else {
            // read-write
            if (!currentTransactions.containsKey(transactionID)) {
                //check if this transaction alive or not
                System.out.println("[Failure] Please make sure that you first announce the BEGIN of this transaction!");
                return;
            }

            // if there is any up site available for reading this variable
            boolean canFindThisVariable = false;
            for (int i = 1; i < DEFAULT_SITE_TOTAL_NUMBER; i++) {
                Site tempSite = this.sites.get(i);
                if (!tempSite.getIfSiteWorking()) {
                    continue;
                }
                if (tempSite.ifContainsVariable(variableID) && tempSite.ifThisVariableIsAvailable(variableID)) {
                    canFindThisVariable = true;
                    break;
                }
            }
            if (!canFindThisVariable) {
                //cannot find any up sites that contains this variable
                insertToWaitList(new Operation(0, variableID, time, TypeOfOperation.OP_READ), transactionID);
                System.out.println("[Failure] Your required variable x" + variableID + " is not available at this time. Please wait!");
                return;
            }

            // if this transaction has already has all write locks
            // if all up site that has this variable all have a write lock hold by this transaction
            boolean ifAllHaveAWriteLock = true;
            for (int i = 1; i <= DEFAULT_SITE_TOTAL_NUMBER; i++) {
                Site tempSite = this.sites.get(i);
                if (!tempSite.getIfSiteWorking()) {
                    continue;
                }
                if (!tempSite.ifHaveThisVariable(variableID)) {
                    continue;
                }
                LockTable tempLockTable = tempSite.getLockTableOfSite();
                if (!tempLockTable.ifTransactionHasLockOnVariableInThisTable(variableID, transactionID, TypeOfLock.Write)) {
                    ifAllHaveAWriteLock = false;
                    break;
                }
            }

            if (ifAllHaveAWriteLock) {
                //This transaction has all the write locks that it needs, which means it can write now.
                int value = getVariableValueFromAnyUpSite(variableID);
                System.out.println("[Success] The value of variable x" + variableID + " that is read is " + value + ".");
                // do not need to give read lock since write lock is already there
                Operation op = new Operation(value, variableID, time, TypeOfOperation.OP_READ);
                transaction.addToOperationHistory(op);
                return;
            }

            //judge if can have all read locks
            boolean ifExistsAnyConlictingWriteLockOnAllUpSites = findifExistsAnyConlictingWriteLockOnAllUpSites(transactionID, variableID);
            if (!ifExistsAnyConlictingWriteLockOnAllUpSites) {
                //there is no conflicting locks, so just read value and get all variable has read lock
                getAllReadLockedOnAllUpSitesByThisTransaction(transactionID, variableID);
                int value = getVariableValueFromAnyUpSite(variableID);
                System.out.println("[Success] The value of variable x" + variableID + " that is read is " + value + ".");
                Operation op = new Operation(value, variableID, time, TypeOfOperation.OP_READ);
                transaction.addToOperationHistory(op);
                return;
            }

            //TODO
            //There is some conflicting lock
            insertToWaitList(new Operation(0, variableID, time, TypeOfOperation.OP_READ), transactionID);
            System.out.println("[Failure] R(T" + transactionID + ", x" + variableID + ") has to wait because it cannot acquire the read lock on that variable.");
            /*
            //oldddddddddddddddddddddddddddddddddddddddddddd
            if (ifTransactionHoldReadLockOnVariable(transaction, variableID)) {
                int index = -1;
                for (int q = 1; q <= DEFAULT_SITE_TOTAL_NUMBER; q++) {
                    if (!this.sites.get(q).getIfSiteWorking()) {
                        if (this.sites.get(q).getLockTableOfSite().
                                ifTransactionHasLockOnVariableInThisTable(variableID, transactionID, TypeOfLock.Read)) {
                            index = q;
                            break;
                        }
                    }
                }
                System.out.println("index:" + index);
                List<Variable> varsInSite = this.sites.get(index).getALLVariables();
                for (Variable temp : varsInSite) {
                    if (temp.getID() == variableID) {
                        System.out.println("[Success] The value of variable x" + variableID + " that is read is " + temp.getValue() + ".");
                        break;
                    }
                }
            } else {
                System.out.println("No read lock found!");
                //the transaction does not have a read lock on this variable
                boolean alreadyRead = false;
                for (int i = 1; i <= DEFAULT_SITE_TOTAL_NUMBER && !alreadyRead; i++) {
                    Site tempSite = sites.get(i);
                    if (tempSite.getIfSiteWorking()) {
                        List<Variable> variablesInThisSite = tempSite.getALLVariables();
                        for (Variable var : variablesInThisSite) {
                            if (var.getID() == variableID && var.isAvailableForReading()) {
                                //variable we are looking for found in this site and it is available for reading(in term of recovery)
                                if (tempSite.getLockTableOfSite().ifCanHaveReadLockOnVariable(variableID, transactionID)) {
                                    //TODO
                                    if (tempSite.getLockTableOfSite().ifTransactionHasLockOnVariableInThisTable(variableID, transactionID, TypeOfLock.Write)) {
                                        //if this transaction has already has a write lock on this variable at this site
                                        alreadyRead = true;
                                        Variable currentVariable = tempSite.getVariableAndID(variableID);
                                        Operation op = new Operation(currentVariable.getCurrValue(), variableID, time, TypeOfOperation.OP_READ);
                                        transaction.addToOperationHistory(op);
                                        transaction.getSitesAccessed().add(i);
                                        System.out.println("[Success] The value of variable x" + variableID + " that is read is "
                                                + currentVariable.getCurrValue() + ".");
                                        //value read is the current value because transaction already has read/write lock on the variable
                                    } else {
                                        //This transaction has no lock on this variable and this variable is ready to be read
                                        alreadyRead = true;
                                        tempSite.getLockTableOfSite().addLock(variableID, transactionID, TypeOfLock.Read);
                                        Variable currentVariable = tempSite.getVariableAndID(variableID);
                                        Operation op = new Operation(currentVariable.getCurrValue(), variableID, time, TypeOfOperation.OP_READ);
                                        transaction.addToOperationHistory(op);
                                        transaction.getSitesAccessed().add(i);
                                        transaction.addLockTolocksList(variableID, TypeOfLock.Read);
                                        System.out.println("[Success] The value of variable x" + variableID + " that is read is "
                                                + currentVariable.getCurrValue() + ".");
                                    }
                                } else {
                                    //This transaction cannot have a read lock on this variable
                                    //This operation has to wait
                                    alreadyRead = true;
                                    insertToWaitList(new Operation(0, variableID, time, TypeOfOperation.OP_READ), transactionID);
                                    System.out.println("[Failure] R(T" + transactionID + ", x" + variableID + ") has to wait because it cannot acquire the read lock on that variable.");
                                }
                            }
                        }
                    }
                }

                if (!alreadyRead) {
                    // The value of this variable could not be read from any up site. So this operation has to wait.
                    // TODO
                    insertToWaitList(new Operation(0, variableID, time, TypeOfOperation.OP_READ), transactionID);
                    System.out.println("[Failure] Your required variable x" + variableID + " is not available at this time. Please wait!");
                }
            }
            */
        }
    }

    private void getAllReadLockedOnAllUpSitesByThisTransaction(int transactionID, int variableID) {
        for (int i = 1; i <= DEFAULT_SITE_TOTAL_NUMBER; i++) {
            Site tempSite = this.sites.get(i);
            if (tempSite.getIfSiteWorking() && tempSite.ifContainsVariable(variableID) && tempSite.ifThisVariableIsAvailable(variableID)) {
                List<LockOnVariable> lockListOnThisVariable = tempSite.getLockTableOfSite().getAllLocksOnVariable(variableID);
                if (lockListOnThisVariable.size() == 0) {
                    tempSite.getLockTableOfSite().addLock(variableID, transactionID, TypeOfLock.Read);
                    System.out.println("read lock added!!!!!!!!");
                }// else means this transaction already has one lock on this variable
            }
        }
    }

    private boolean findifExistsAnyConlictingWriteLockOnAllUpSites(int transactionID, int variableID) {
        for (int i = 1; i <= DEFAULT_SITE_TOTAL_NUMBER; i++) {
            System.out.println("Looking at site" + i + ":");
            Site tempSite = this.sites.get(i);
            if (tempSite.getIfSiteWorking() && tempSite.ifContainsVariable(variableID)) {
                System.out.println("Entering site" + i + ":");
                List<LockOnVariable> lockListOnThisVariable = tempSite.getLockTableOfSite().getAllLocksOnVariable(variableID);
                for (LockOnVariable lock : lockListOnThisVariable) {
                    if (lock.getVariableID() == variableID && lock.getTransactionID() != transactionID && lock.getLockType() != TypeOfLock.Read) {
                        System.out.println("Transaction T" + transactionID + " is blocked by Transaction T" + lock.getTransactionID() + " on Variable x" + variableID + ".");
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private int getVariableValueFromAnyUpSite(int variableID) {
        for (int i = 1; i <= DEFAULT_SITE_TOTAL_NUMBER; i++) {
            Site tempSite = this.sites.get(i);
            if (!tempSite.getIfSiteWorking()) {
                continue;
            }
            if (!tempSite.ifHaveThisVariable(variableID)) {
                continue;
            }

            return tempSite.returnThisVariableValue(variableID);
        }
        return Integer.MIN_VALUE;
    }


    private boolean ifTransactionHoldReadLockOnVariable(Transaction t, int variableID) {
        List<LockOnVariable> transactionLockList = t.getLocksList();
        for (int i = 0; i < transactionLockList.size(); i++) {
            if (transactionLockList.get(i).getVariableID() == variableID
                    && transactionLockList.get(i).getLockType() == TypeOfLock.Read) {
                System.out.println("Transaction T" + transactionLockList.get(i).getTransactionID() +" has read lock on variable x" + transactionLockList.get(i).getVariableID());
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
            ops = new ArrayList<>();
            ops.add(op);
            waitList.put(transactionID, ops);
        }
    }


    private void writeVariableValue(int transactionID, int variableID, int value) {
        //TODO
        if (!currentTransactions.containsKey(transactionID)) {
            //check if this transaction alive or not
            System.out.println("[Failure] Please make sure that you first announce the BEGIN of this transaction!");
            return;
        }

        Transaction transaction = currentTransactions.get(transactionID);
        if (transaction.getTransactionType() != TypeOfTransaction.Read_Write) {
            System.out.println("[Failure] Please make sure that this transaction is actually a READ_WRITE one!");
            return;
        }

        // if all up site that has this variable all have a write lock hold by this transaction
        boolean ifAllHaveAWriteLock = true;
        for (int i = 1; i <= DEFAULT_SITE_TOTAL_NUMBER; i++) {
            Site tempSite = this.sites.get(i);
            if (!tempSite.getIfSiteWorking()) {
                continue;
            }
            if (!tempSite.ifHaveThisVariable(variableID)) {
                continue;
            }
            LockTable tempLockTable = tempSite.getLockTableOfSite();
            if (!tempLockTable.ifTransactionHasLockOnVariableInThisTable(variableID,transactionID, TypeOfLock.Write)) {
                ifAllHaveAWriteLock = false;
                break;
            }
        }

        //if (transaction.ifAlreadyHaveWriteLock(variableID) && ifAllHaveAWriteLock) {
        if (ifAllHaveAWriteLock) {
//            int numberOfWriteLocksOnThisVariableByThisTransaction = transaction
//                    .getNumberOfLocksOnThisVariableByThisTransaction(TypeOfLock.Write, variableID);
//            int numberOfUpSitesContainingThisVariable = this.getNumberOfUpSitesContainingThisVariable(variableID);
            //This transaction has all the write locks that it needs, which means it can write now.
            System.out.println("[Success] Variable x" + variableID
                    + " on all up sites has their temp uncommitted value to be " + value
                    + " by transaction T" + transactionID + " and it have already had all the locks.");
            this.writeToAllUpSites(transactionID, variableID, value);
            Operation op = new Operation(value, variableID, time, TypeOfOperation.OP_WRITE);
            transaction.addToOperationHistory(op);
        } else {
            //boolean ifThisTransactionCanHaveWriteLockOnAllUpSites = findIfExistsConflictLockOnAllUpSites(transactionID, variableID);
            System.out.println("T" + transactionID + " & " + "x" + variableID);
            if (!findIfExistsConflictLockOnAllUpSites(transactionID, variableID)) {
                getAllWriteLockedOnAllUpSitesByThisTransaction(transactionID, variableID);
                this.writeToAllUpSites(transactionID, variableID, value);
                Operation op = new Operation(value, variableID, time, TypeOfOperation.OP_WRITE);
                transaction.addToOperationHistory(op);
                System.out.println("[Success] Variable x" + variableID
                        + " on all up sites has their temp uncommitted value to be " + value
                        + " by transaction T" + transactionID + " and it has already managed to get all the locks.");
            }
            else {
                // put this transaction to the waiting list
                System.out.println("//TODO putting this transcation into the waiting list!");
                // TODO
            }
        }

    }

    private void getAllWriteLockedOnAllUpSitesByThisTransaction(int transactionID, int variableID) {
        for (int i = 1; i <= DEFAULT_SITE_TOTAL_NUMBER; i++) {
            Site tempSite = this.sites.get(i);
            if (tempSite.getIfSiteWorking() && tempSite.ifContainsVariable(variableID)) {
                List<LockOnVariable> lockListOnThisVariable = tempSite.getLockTableOfSite().getAllLocksOnVariable(variableID);
                if (lockListOnThisVariable.size() == 0) {
                    tempSite.getLockTableOfSite().addLock(variableID, transactionID, TypeOfLock.Write);
                    System.out.println("added!!!!!!!!");
                } else {
                    tempSite.getLockTableOfSite().updateReadLockToWriteLock(variableID, transactionID);
                    System.out.println("updated!!!!!!!!");
                }
            }
        }
    }

    private boolean findIfExistsConflictLockOnAllUpSites(int transactionID, int variableID) {
        //find if exists any read or write lock hold by other transaction on this variable
        for (int i = 1; i <= DEFAULT_SITE_TOTAL_NUMBER; i++) {
            System.out.println("Looking at site" + i + ":");
            Site tempSite = this.sites.get(i);
            if (tempSite.getIfSiteWorking() && tempSite.ifContainsVariable(variableID)) {
                System.out.println("Entering site" + i + ":");
                List<LockOnVariable> lockListOnThisVariable = tempSite.getLockTableOfSite().getAllLocksOnVariable(variableID);
                for (LockOnVariable lock : lockListOnThisVariable) {
                    if (lock.getVariableID() == variableID && lock.getTransactionID() != transactionID) {
                        System.out.println("Transaction T" + transactionID + " is blocked by Transaction T" + lock.getTransactionID() + " on Variable x" + variableID + ".");
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void writeToAllUpSites(int transactionID, int variableID, int value) {
        for (int i = 1; i <= DEFAULT_SITE_TOTAL_NUMBER; i++) {
            if (this.sites.get(i).getIfSiteWorking() && this.sites.get(i).ifContainsVariable(variableID)
                    && this.sites.get(i).getLockTableOfSite().ifThisTransactionHasWriteLockInThisLockTable(transactionID)) {
                this.sites.get(i).writeToVariableCurrValueInThisSite(variableID, value);
            }
        }
    }

    private int getNumberOfUpSitesContainingThisVariable(int variableID) {
        int result = 0;
        for (int i = 1; i <= DEFAULT_SITE_TOTAL_NUMBER; i++) {
            if (this.sites.get(i).getIfSiteWorking()) {
                List<Variable> variableList = this.sites.get(i).getALLVariables();
                for (Variable var : variableList) {
                    if (var.getID() == variableID) {
                        result++;
                    }
                }
            }
        }
        return result;
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