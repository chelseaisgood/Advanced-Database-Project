package edu.nyu.csciga2434.project;

import java.util.*;

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
    private static final int DEFAULT_VARIABLE_TOTAL_NUMBER = 20;

    private Map<Integer, Site> sites;
    private Map<Integer, Transaction> currentTransactions;
    private int time;
    private Set<Integer> abortedTransactions;
    public List<BufferedOperation> bufferedWaitList;
    private List<WaitFor> waitForList;

    public TransactionManager() {
        this.sites = new HashMap<>();
        this.time = 0;
        for (int i = 1; i <= DEFAULT_SITE_TOTAL_NUMBER; i++) {
            sites.put(i, new Site(i));
        }
        abortedTransactions = new HashSet<>();
        currentTransactions = new HashMap<>();
        bufferedWaitList = new ArrayList<>();
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
                endTransactionList.add(Integer.parseInt(op.substring(5, op.length() - 1)));
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
                System.out.println("[Success] Read-only transaction T" + transactionID + " has been successfully initiated.");
            } else {
                System.out.println("[Success] Read-write transaction T" + transactionID + " has been successfully initiated.");
            }
        } else {
            System.out.println("[Failure] Transaction T" + transactionID + " might be already in progress.");
        }
    }

    private boolean hasAborted(int tid) {
        return abortedTransactions.contains(tid);
    }

    private void endTransaction(int transactionID) {

        if (!this.currentTransactions.containsKey(transactionID)) {
            System.out.println("No such transaction T" + transactionID + " to end!");
            return;
        }

        if (hasAborted(transactionID)) {
            System.out.println("[Aborted] This transaction T" + transactionID + " has been aborted!");
            return;
        }

        Transaction transactionToBeEnded = this.currentTransactions.get(transactionID);

        if (transactionToBeEnded.getTransactionType() == TypeOfTransaction.Read_Only) {
            System.out.println("Read-only transaction T" + transactionID + " has been ended.");
            return;
        }

        //transaction to be ended is a read_write one
        if (waitList != null && waitList.containsKey(transactionID) && waitList.get(transactionID).size() != 0) {
            System.out.println("[Blocked] This transaction T" + transactionID + " has been blocked!");
            return;
        }

        for (int i = 1; i <= DEFAULT_SITE_TOTAL_NUMBER; i++) {
            Site tempSite = this.sites.get(i);
            List<LockOnVariable> table = tempSite.getLockTableOfSite().lockTable;
            List<Integer> indexList= new ArrayList<>();
            for (int j = 0; j < table.size(); j++) {
                LockOnVariable lock = table.get(j);
                String typeOfLock = (lock.getLockType() == TypeOfLock.Read) ? "Read" : "Write";
                System.out.println("At site " + tempSite.getSiteID()
                        + ", Transaction T" + lock.getTransactionID()
                        + " holds a " + typeOfLock + " lock on variable x" + lock.getVariableID() + ".");
                if (lock.getTransactionID() != transactionID) {
                    continue;
                }

                if (lock.getLockType() == TypeOfLock.Read) {
                    //tempSite.ReleaseThatLock(lock);
                    indexList.add(j);
                    continue;
                }

                if (lock.getLockType() == TypeOfLock.Write) {
                    System.out.println("Starting to release write lock on variable x" + lock.getVariableID()
                            + " held by Transaction T" + lock.getTransactionID() + "." );
                    tempSite.CommitTheWrite(lock);
                    indexList.add(j);
                    //tempSite.ReleaseThatLock(lock);
                }
            }

            Collections.sort(indexList);

            for (int k = indexList.size() - 1; k >= 0; k--) {
                tempSite.ReleaseThatLock(table.get(k));
            }

        }
        System.out.println("[Committed] This transaction T" + transactionID + " has been committed!");

    }


    private void failSite(int siteID) {
        if (this.sites.containsKey(siteID) && sites.get(siteID).getIfSiteWorking() == true) {
            sites.get(siteID).failThisSite();
        } else {
            System.out.println("[Failure] Fail to fail this site. Maybe it is still down or not even exists!");
        }
    }

    private void recoverSite(int siteID) {
        if (this.sites.containsKey(siteID) && sites.get(siteID).getIfSiteWorking() == false) {
            sites.get(siteID).recoverThisSite();
        } else {
            System.out.println("[Failure] Fail to recover this site. Maybe it is still working or not even exists!");
        }
    }

    private void readVariableValue(int transactionID, int variable) {
        if (currentTransactions.containsKey(transactionID) && variable >= 1 && variable <= DEFAULT_VARIABLE_TOTAL_NUMBER) {
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
            for (int i = 1; i <= DEFAULT_SITE_TOTAL_NUMBER; i++) {
                Site tempSite = sites.get(i);
                if (!tempSite.getIfSiteWorking()) {
                    continue;
                }
                List<Variable> variablesInThisSite = tempSite.getALLVariables();
                for (Variable var : variablesInThisSite) {
                    if (var.getID() == variableID && var.isAvailableForReading()) {
                        //parse the VariableHistory of this Variable and select the correct value to read
                        List<VariableHistory> variableHistory = var.getVariableHistoryList();
                        int maxTime = Integer.MIN_VALUE; //time counter
                        int maxIndex = -1;  //index in the List which has max time
                        for (int j = 0; j < variableHistory.size(); j++) {
                            VariableHistory vHistoryTemp = variableHistory.get(j);
                            if (vHistoryTemp.getTime() > maxTime && vHistoryTemp.getTime() < transaction.getStartTime()) {
                                maxIndex = j;
                                maxTime = vHistoryTemp.getTime();
                            }
                        }
                        if (maxIndex != -1) {
                            int readValue = variableHistory.get(maxIndex).getValue();
                            System.out.println("[Success] The snapshot value of variable x" + variableID + " is " + readValue + ".");
                            Operation op = new Operation(variableID, TypeOfOperation.OP_READ, i, variableID, readValue, time);
                            // TODO
                            transaction.addToOperationHistory(op);
                            return;
                            // put successful operation into operationHistory record stored inside every transaction
                        }
                    }
                }
            }

            // The value of this variable could not be read from any up site. So this operation has to wait.
            // TODO
            insertToWaitList(new BufferedOperation(TypeOfBufferedOperation.VariableUnavaiable, transactionID, transactionID, variableID, typeOfTransaction, TypeOfOperation.OP_READ, time));
            System.out.println("[Failure] Your required variable x" + variableID + " is not available at this time. Please wait!");

        } else {
            // read-write
            if (!currentTransactions.containsKey(transactionID)) {
                //check if this transaction alive or not
                System.out.println("[Failure] Please make sure that you first announce the BEGIN of this transaction!");
                return;
            }

            // check if there is any buffered operations left in the buffered operation list with the query on the same variable
            // new TODO
            ConflictingBufferedQueryReturn queryReturn = findExistingConflictingWrittingBufferedOperation(variableID);

            if (queryReturn.getIfExistsAnyConflictingBufferedOperations()) {
                int waitForTransactionID = queryReturn.getBufferedConflictingTransactionID();
                insertToWaitList(new BufferedOperation(TypeOfBufferedOperation.TransactionBlocked, transactionID, waitForTransactionID, variableID, typeOfTransaction, TypeOfOperation.OP_READ, time));
                // TODO put this wait-for relation in the wait for list
                waitForList.add(new WaitFor(transactionID, waitForTransactionID, getTransactionStartTime(transactionID)));
                System.out.println("[Failure] Your required variable x" + variableID + " is not available at this time. Please wait!");
                return;
            }

            // if there is any up site available for reading this variable
            boolean canFindThisVariable = false;
            for (int i = 1; i <= DEFAULT_SITE_TOTAL_NUMBER; i++) {
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
                insertToWaitList(new BufferedOperation(TypeOfBufferedOperation.VariableUnavaiable, transactionID, transactionID, variableID, typeOfTransaction, TypeOfOperation.OP_READ, time));
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
                ReadReturn readReturn = readCurrentValueOfVariableFromOneUpSite(variableID);
                int readValue = readReturn.getReadValue();
                int siteID = readReturn.getSiteNumber();
                System.out.println("[Success] The value of variable x" + variableID + " that is read is " + readValue + ".");
                // do not need to give read lock since write lock is already there
                Operation op = new Operation(variableID, TypeOfOperation.OP_READ, siteID, variableID, readValue, time);
                transaction.addToOperationHistory(op);
                return;
            }

            //judge if can have all read locks

            boolean ifExistsAnyConflictingWriteLockOnAllUpSites = findifExistsAnyConflictingWriteLockOnAllUpSites(transactionID, variableID);
            if (!ifExistsAnyConflictingWriteLockOnAllUpSites) {
                //there is no conflicting locks, so just read value and get one variable to have a read lock
                getAllReadLockedOnAllUpSitesByThisTransaction(transactionID, variableID);
                //int value = getVariableValueFromAnyUpSite(variableID);

                ReadReturn readReturn = readCurrentValueOfVariableFromOneUpSite(variableID);
                int readValue = readReturn.getReadValue();
                int siteID = readReturn.getSiteNumber();

                // Operation(int transactionID, TypeOfOperation operationType, int siteID, int variableID, int value, int time)
                Operation op = new Operation(transactionID, TypeOfOperation.OP_READ, siteID, variableID, readValue, time);
                transaction.addToOperationHistory(op);
                System.out.println("[Success] The value of variable x" + variableID + " that is read is " + readValue + ".");
                return;
            }


            //TODO
            //There is some conflicting lock
            ConflictingBufferedQueryReturn queryReturnNew = findExistingAnyConflictingWriteLockOnAllUpSites(transactionID, variableID);
            int blockedTransactionID = queryReturnNew.getBufferedConflictingTransactionID();

            insertToWaitList(new BufferedOperation(TypeOfBufferedOperation.TransactionBlocked, transactionID, blockedTransactionID, variableID, typeOfTransaction, TypeOfOperation.OP_READ, time));
            // TODO put this wait-for relation in the wait for list
            waitForList.add(new WaitFor(transactionID, blockedTransactionID, getTransactionStartTime(transactionID)));
            System.out.println("[Failure] R(T" + transactionID + ", x" + variableID + ") has to wait because it cannot acquire the read lock on that variable.");
            return;
        }
    }

    private ConflictingBufferedQueryReturn findExistingAnyConflictingWriteLockOnAllUpSites(int transactionID, int variableID) {
        for (int i = 1; i <= DEFAULT_SITE_TOTAL_NUMBER; i++) {
            //System.out.println("Looking at site" + i + ":");
            Site tempSite = this.sites.get(i);
            if (tempSite.getIfSiteWorking() && tempSite.ifContainsVariable(variableID)) {
                //System.out.println("Entering site" + i + ":");
                List<LockOnVariable> lockListOnThisVariable = tempSite.getLockTableOfSite().getAllLocksOnVariable(variableID);
                for (LockOnVariable lock : lockListOnThisVariable) {
                    if (lock.getVariableID() == variableID && lock.getTransactionID() != transactionID && lock.getLockType() != TypeOfLock.Read) {
                        System.out.println("Transaction T" + transactionID + " is blocked by Transaction T" + lock.getTransactionID() + " on Variable x" + variableID + ".");
                        return new ConflictingBufferedQueryReturn(true, lock.getTransactionID());
                    }
                }
            }
        }
        return new ConflictingBufferedQueryReturn(false, -1);
    }

    private int getTransactionStartTime(int transactionID) {
        if (currentTransactions.containsKey(transactionID)) {
            return currentTransactions.get(transactionID).getStartTime();
        }
        return 0;
    }

    private ConflictingBufferedQueryReturn findExistingConflictingWrittingBufferedOperation(int variableID) {
        // BufferedOperation(TypeOfBufferedOperation typeOfBufferedOperation, int transactionID, int variableID,
        //                      TypeOfTransaction typeOfTransaction, TypeOfOperation typeOfOperation, int bufferedTime)
        for (BufferedOperation BO : bufferedWaitList) {
            if (BO.getVariableID() == variableID && BO.getTypeOfOperation() == TypeOfOperation.OP_WRITE) {
                return new ConflictingBufferedQueryReturn(true, BO.getTransactionID());
            }
        }
        return null;
    }

    private ReadReturn readCurrentValueOfVariableFromOneUpSite(int variableID) {
        for (int i = 1; i <= DEFAULT_SITE_TOTAL_NUMBER; i++) {
            Site tempSite = this.sites.get(i);
            if (!tempSite.getIfSiteWorking()) {
                continue;
            }
            if (!tempSite.ifHaveThisVariable(variableID)) {
                continue;
            }

            int currentValueRead = tempSite.returnThisVariableCurrentValue(variableID);
            return new ReadReturn(i, currentValueRead);
        }
        return null;
    }

    private void getAllReadLockedOnAllUpSitesByThisTransaction(int transactionID, int variableID) {
        for (int i = 1; i <= DEFAULT_SITE_TOTAL_NUMBER; i++) {
            Site tempSite = this.sites.get(i);
            if (tempSite.getIfSiteWorking() && tempSite.ifContainsVariable(variableID) && tempSite.ifThisVariableIsAvailable(variableID)) {
                List<LockOnVariable> lockListOnThisVariable = tempSite.getLockTableOfSite().getAllLocksOnVariable(variableID);
                if (lockListOnThisVariable.size() == 0) {
                    tempSite.getLockTableOfSite().addLock(variableID, transactionID, TypeOfLock.Read);
                    System.out.println("read lock added!!!!!!!!");
                    break;
                }// else means this transaction already has one lock on this variable
            }
        }
    }

    private boolean findifExistsAnyConflictingWriteLockOnAllUpSites(int transactionID, int variableID) {
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

    /*
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
    */

    private void insertToWaitList(BufferedOperation bufferedOperation) {
        bufferedWaitList.add(bufferedOperation);
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
                insertToWaitList(transactionID, new Operation(value, variableID, time, TypeOfOperation.OP_WRITE));
                System.out.println("[Failure] Your required variable x" + variableID + " is not available at this time. Please wait!");
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


    /**Parts with the deadlock checking
     * 1. given wait-for lists
     * 2. generate matrix
     * 3. if exists cycle {
     * 4.   find one path
     * 5.   find one to abort
     * 6.   export that one to abortlist
     * 7.   revise the matrix
     * 8. }
     */

    private static List<Integer> deadLockRemoval(List<WaitFor> waitForList) {
        Set<Integer> set = new HashSet<>();
        List<Integer> list = new ArrayList<>();

        for (WaitFor waitFor : waitForList) {
            int from  = waitFor.getFrom();
            int to  = waitFor.getTo();
            if (!set.contains(from)) {
                set.add(from);
                list.add(from);
            }
            if (!set.contains(to)) {
                set.add(to);
                list.add(to);
            }
        }

        Collections.sort(list);

        int sizeOfMat = list.size();
        int sizeOfWaitForList = waitForList.size();
        System.out.println("Size of this list is " + sizeOfMat + ".");
        int[][] mat = new int[sizeOfMat][sizeOfMat];

        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < sizeOfMat; i++) {
            map.put(list.get(i), i);
        }

        for (int i = 0; i < sizeOfWaitForList; i++) {
            WaitFor waitFor = waitForList.get(i);
            int from  = waitFor.getFrom();
            int to  = waitFor.getTo();
            //System.out.println("From " + from + " to " + to + ".");
            //System.out.println("From " + map.get(from) + " to " + map.get(to) + ".");
            mat [map.get(from)][map.get(to)] = 1;
        }

        printMatrix(mat);

        List<Integer> abortTransactionIDList = new ArrayList<>();

        Map<Boolean, Set<Integer>> cycleCheckResult = checkCycle(mat);
        while (cycleCheckResult.containsKey(true)) {
            System.out.println("A cycle is found!");
            Set<Integer> tempHashSet = cycleCheckResult.get(true);
            Set<Integer> translatedSet = translateSet(tempHashSet, list);

            int abortedTransactionID = -1;
            int maxTime = -1;
            for (int index : translatedSet) {
                int translatedIndex = index;
                int tempTime = getWaitForTime (waitForList, translatedIndex);
                if (tempTime > maxTime) {
                    abortedTransactionID = translatedIndex;
                }
            }
            System.out.println("abortedTransactionID is Index #" + abortedTransactionID + "!" );
            abortTransactionIDList.add(abortedTransactionID);
            System.out.println("Setting both row and column #" + map.get(abortedTransactionID) + " to be zero!" );
            reviceTheMat(mat, map.get(abortedTransactionID));
            printMatrix(mat);
            cycleCheckResult = checkCycle(mat);
        }

        printMatrix(mat);

        return abortTransactionIDList;
    }

    private static void reviceTheMat(int[][] mat, int clearID) {
        int size = mat.length;
        for (int i = 0; i < size; i++) {
            mat[i][clearID] = 0;
            mat[clearID][i] = 0;
        }
        return;
    }

    private static int getWaitForTime(List<WaitFor> waitForList, int translatedIndex) {
        for (WaitFor waitFor : waitForList) {
            if (waitFor.getFrom() == translatedIndex) {
                return waitFor.getTime();
            }
        }
        return -1;
    }

    private static Set<Integer> translateSet(Set<Integer> tempHashSet, List<Integer> list) {
        Set<Integer> result = new HashSet<>();
        for (int index : tempHashSet) {
            int translatedIndex = list.get(index);
            result.add(translatedIndex);
            System.out.println("Translated Index #" + translatedIndex + " is translated!" );
        }
        System.out.println("One translation is done!");
        return result;
    }

    private static Map<Boolean, Set<Integer>> checkCycle(int[][] mat) {
        int size = mat.length;
        List<Vertex> V= new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Vertex vertex = new Vertex();
            V.add(vertex);
        }

        for (int index = 0; index < size; index++) {
            Vertex tempVertex = V.get(index);
            if (tempVertex.getColor() == Color.white) {
                System.out.println("New HashSet is created!");
                Set<Integer> tempSet = new HashSet<>();
                int tempIndex = index;
                if (tempSet.contains(tempIndex)) {
                    Map<Boolean, Set<Integer>> retMap  = new HashMap<>();
                    retMap.put(true, tempSet);
                    return retMap;
                }
                tempSet.add(tempIndex);
                System.out.println("Index #" + tempIndex + " is added.");
                tempVertex.setColor(Color.black);
                int[] tempRow = mat[tempIndex];
                int nextNodeIndex = getNextIndex(tempRow);
                while (nextNodeIndex >= 0) {
                    tempIndex = nextNodeIndex;
                    if (tempSet.contains(tempIndex)) {
                        Map<Boolean, Set<Integer>> retMap  = new HashMap<>();
                        retMap.put(true, tempSet);
                        return retMap;
                    }
                    tempSet.add(tempIndex);
                    System.out.println("Index #" + tempIndex + " is added.");
                    tempVertex = V.get(tempIndex);
                    tempVertex.setColor(Color.black);
                    tempRow = mat[tempIndex];
                    nextNodeIndex = getNextIndex(tempRow);
                }
            }
        }

        Map<Boolean, Set<Integer>> finalMap  = new HashMap<>();
        finalMap.put(false, null);
        return finalMap;
    }

    private static int getNextIndex(int[] tempRow) {
        for (int i = 0; i < tempRow.length; i++) {
            if (tempRow[i] != 0) {
                return i;
            }
        }
        return -1;
    }


    private static void printMatrix (int[][] mat) {
        int size = mat.length;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (j == size - 1) {
                    System.out.println(mat[i][j]);
                    continue;
                }
                System.out.print(mat[i][j] + " ");
            }
        }
    }



    //keep this part for backup
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