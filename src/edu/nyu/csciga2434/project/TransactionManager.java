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
    private Set<Integer> committedTransactions;
    private Set<Integer> abortedTransactions;
    private List<BufferedOperation> bufferedWaitList;
    private List<WaitFor> waitForList;
    private Set<Integer> toBeAbortedList;
    private Map<Integer, List<Operation>> SiteTransactionHistory;

    public List<BufferedOperation> getBufferedWaitList() {
        return this.bufferedWaitList;
    }

    public TransactionManager() {
        this.sites = new HashMap<>();
        this.time = 0;
        this.SiteTransactionHistory = new HashMap<>();
        for (int i = 1; i <= DEFAULT_SITE_TOTAL_NUMBER; i++) {
            sites.put(i, new Site(i));
            this.SiteTransactionHistory.put(i, new ArrayList<>());
        }
        this.abortedTransactions = new HashSet<>();
        this.committedTransactions = new HashSet<>();
        this.currentTransactions = new HashMap<>();
        this.bufferedWaitList = new ArrayList<>();
        this.waitForList = new ArrayList<>();
        this.toBeAbortedList = new HashSet<>();
    }


    public void readCommand(String commandLine) {
        /**
         *  1. process those buffered operations
         *  2. process commands in this line
         *  3. check deadlock
         */
        this.time++;    // next tick
        System.out.println("\n" + "[New Round] Time " + time);

        List<BufferedOperation> oldBufferedWaitList = bufferedWaitList; //a copy of the old buffered wait list
        bufferedWaitList = new ArrayList<>();   // recreate the buffered wait list
        waitForList = new ArrayList<>();    // recreate the wait for list

        // processing buffered operations before reading in the next line of commands
        if (oldBufferedWaitList.size() != 0) {
            System.out.println("[Report] The size of the buffered wait list is " + oldBufferedWaitList.size() + " at the end of time " + (time - 1) +".");

            // processing buffered operations according to the sequence of buffered time
            Collections.sort(oldBufferedWaitList, new BufferedOperationComparator());
            oldBufferedWaitList.forEach(this::processThisBufferedOperation);

            System.out.println("[Report] The size of the buffered wait list is " + bufferedWaitList.size() + " at time " + time +" after processing all buffered operations.");
        }

        // process new read in command line
        String[] operations = commandLine.split(";");
        ArrayList<Integer> endTransactionList = new ArrayList<>();
        for (String opRaw : operations) {
            String op = opRaw.trim();
            System.out.println("[New Command] " + op);
            if (op.startsWith("begin(")) {
                begin(Integer.parseInt(op.substring(7, op.length() - 1)), TypeOfTransaction.Read_Write);
            } else if (op.startsWith("beginRO(")) {
                begin(Integer.parseInt(op.substring(9, op.length() - 1)), TypeOfTransaction.Read_Only);
            } else if (op.startsWith("R(")) {
                String[] t = op.substring(2, op.length() - 1).split(",");
                readVariableValue(Integer.parseInt(t[0].substring(1)), Integer.parseInt(t[1].substring(t[1].indexOf("x") + 1)));
            } else if (op.startsWith("W(")) {
                String[] t = op.substring(2, op.length() - 1).split(",");
                writeVariableValue(Integer.parseInt(t[0].substring(1)), Integer.parseInt(t[1].substring(t[1].indexOf("x") + 1)), Integer.valueOf(t[2].trim()), time);
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

        //printSiteTransactionHistory();
        printToBeAbortedList();

        this.toBeAbortedList.forEach(this::abort);
        // reset to be aborted list before deadlock checking
        this.toBeAbortedList = new HashSet<>();

        endTransactionList.forEach(this::endTransaction);

        List<Integer> deadLockAbortTransactionIDList = deadLockRemoval(waitForList);
        deadLockAbortTransactionIDList.forEach(this::abort);
        //printSiteLockTable();
    }


    /**
     *  This function process one buffered operation by simply re-executing it.
     *  Buffered time is kept in order not to lose the priority info of this operation.
     *  This function is called at the start of function readCommand
     */
    private void processThisBufferedOperation(BufferedOperation bo) {
        // BufferedOperation(TypeOfBufferedOperation typeOfBufferedOperation, int transactionID,
        //                      int previousWaitingTransactionID, int variableID,
        //                      TypeOfTransaction typeOfTransaction, TypeOfOperation typeOfOperation,
        //                      int bufferedTime)

        System.out.println("[Report] Now processing the operation buffered at time " + bo.getBufferedTime()
                + " which is the " + bo.getTypeOfTransaction() + " transaction T"
                + bo.getTransactionID() + " asking to " + bo.getTypeOfOperation()
                + " on variable x" + bo.getVariableID() + " with value " + bo.getValue()
                + " but previously blocked by Transaction T"
                + bo.getPreviousWaitingTransactionID() + ".");

        if (bo.getTypeOfOperation() == TypeOfOperation.OP_READ) {
            read(bo.getTransactionID(), bo.getVariableID(), bo.getTypeOfTransaction(), bo.getBufferedTime());
        } else {
            writeVariableValue(bo.getTransactionID(), bo.getVariableID(), bo.getValue(), bo.getBufferedTime());
        }
    }


    /**
     *  Begin transaction as ordered.
     *  Put successfully initiated transactions into current transaction hash map.
     */
    private void begin(int transactionID, TypeOfTransaction typeOfTransaction) {
        if (!currentTransactions.containsKey(transactionID)) {
            Transaction newTransaction = new Transaction(transactionID, typeOfTransaction, time);
            currentTransactions.put(transactionID, newTransaction);
            if (typeOfTransaction == TypeOfTransaction.Read_Only) {
                System.out.println("[Success] Read-only transaction T" + transactionID + " initiated.");
            } else {
                System.out.println("[Success] Read-write transaction T" + transactionID + " initiated.");
            }
        } else {
            System.out.println("[Failure] Transaction T" + transactionID + " might be already in progress.");
        }
    }


    /**
     *  Check validity before actually read the variable value
     *  Operation time is passed in order not to lose the priority of this operation
     */
    private void readVariableValue(int transactionID, int variable) {
        if (currentTransactions.containsKey(transactionID) && variable >= 1 && variable <= DEFAULT_VARIABLE_TOTAL_NUMBER) {
            Transaction t = currentTransactions.get(transactionID);
            if (t.getTransactionType() == TypeOfTransaction.Read_Only) {
                read(transactionID, variable, TypeOfTransaction.Read_Only, time);
            } else {
                read(transactionID, variable, TypeOfTransaction.Read_Write, time);
            }
        } else {
            System.out.println("[Failure] Please check if such transaction T" + transactionID + " has began or such variable x" + variable + " exists.");
        }
    }


    /**
     *  Read the variable value
     */
    private void read(int transactionID, int variableID, TypeOfTransaction typeOfTransaction, int opTime) {
        // don't need to check the validity of transaction ID and variable ID
        //      since it is already done before calling this function
        Transaction transaction = this.currentTransactions.get(transactionID);
        if (typeOfTransaction == TypeOfTransaction.Read_Only) {
            for (int siteID = 1; siteID <= DEFAULT_SITE_TOTAL_NUMBER; siteID++) {
                Site tempSite = sites.get(siteID);
                // bypass the not working site
                if (!tempSite.getIfSiteWorking()) {
                    continue;
                }
                // get the variable list in this site
                List<Variable> variablesInThisSite = tempSite.getALLVariables();
                for (Variable var : variablesInThisSite) {
                    // variable which is not available for read will be ignored
                    if (var.getID() == variableID && var.isAvailableForReading()) {
                        // parse the VariableHistory of this Variable and select the correct value to read
                        List<VariableHistory> variableHistory = var.getVariableHistoryList();
                        int maxTime = Integer.MIN_VALUE; //initial return time to be a invalid value first
                        int maxIndex = -1;  //index of the variable history with the latest time
                        for (int j = 0; j < variableHistory.size(); j++) {
                            VariableHistory vHistoryTemp = variableHistory.get(j);
                            // if this variable history has time greater than current recorded max time
                            // and if this variable history has time before the start-time of this read-only transaction
                            // then we should select this record as current optimal choice
                            if (vHistoryTemp.getTime() > maxTime && vHistoryTemp.getTime() < transaction.getStartTime()) {
                                maxIndex = j;
                                maxTime = vHistoryTemp.getTime();
                            }
                        }
                        if (maxIndex != -1) {
                            // get read value from the chosen record in the variable's record history
                            int readValue = variableHistory.get(maxIndex).getValue();
                            System.out.println("[Success] The snapshot value of variable x" + variableID + " is " + readValue + ".");
                            Operation op = new Operation(transactionID, TypeOfOperation.OP_READ, siteID, variableID, readValue, opTime);
                            // put successful operation into SiteTransactionHistory record stored inside this Transaction Manager
                            insertIntoSiteTransactionHistory(siteID, op);
                            // put successful operation into operationHistory record stored inside every transaction
                            transaction.addToOperationHistory(op);
                            return;
                        }
                    }
                }
            }
            // The value of this variable could not be read from any up site. So this operation has to wait.
            // put this operation into buffered operation list
            // set value attribute of this buffered operation for read operation to be zero(don't care)
            insertIntoBufferedWaitList(new BufferedOperation(TypeOfBufferedOperation.VariableUnavailable, transactionID, transactionID, variableID, typeOfTransaction, TypeOfOperation.OP_READ, 0, opTime));
            System.out.println("[Buffered] Variable x" + variableID + " is not available for read-only transaction T" + transactionID + " at this time.");
        } else {
            // read-write transaction read variable value

            // check if there is any buffered operations left in the buffered operation list with the query on the same variable
            // ConflictingBufferedQueryReturn(boolean ifExistsAnyConflictingBufferedOperations, int bufferedConflictingTransactionID)
            ConflictingBufferedQueryReturn queryReturn = findExistingConflictingWritingBufferedOperation(variableID);

            if (queryReturn.getIfExistsAnyConflictingBufferedOperations()) {
                int waitForTransactionID = queryReturn.getBufferedConflictingTransactionID();
                insertIntoBufferedWaitList(new BufferedOperation(TypeOfBufferedOperation.TransactionBlocked, transactionID, waitForTransactionID, variableID, typeOfTransaction, TypeOfOperation.OP_READ, 0, opTime));
                // put this wait-for relation in the wait for list
                insertIntoWaitForRelation(new WaitFor(transactionID, waitForTransactionID, getTransactionStartTime(transactionID)));
                System.out.println("[Buffered] Transaction T" + transactionID + " is blocked by Transaction T" + waitForTransactionID + " on Variable x" + variableID + " in the buffered operation list.");
                return;
            }

            // if there is any up site available for reading this variable
            boolean canFindThisVariable = false;
            for (int siteID = 1; siteID <= DEFAULT_SITE_TOTAL_NUMBER; siteID++) {
                Site tempSite = this.sites.get(siteID);
                if (!tempSite.getIfSiteWorking()) {
                    // ignore not working site
                    continue;
                }
                if (tempSite.ifContainsVariable(variableID) && tempSite.ifThisVariableIsAvailable(variableID)) {
                    canFindThisVariable = true;
                    break;
                }
            }
            if (!canFindThisVariable) {
                // cannot find any up site that contains this variable
                // set previousWaitingTransactionID field to be the same as this transactionID
                //      because this transaction is blocked due to variable unavailable, not because of can acquire the lock on that variable
                //      so this field has not meaning when the TypeOfBufferedOperation is VariableUnavailable
                insertIntoBufferedWaitList(new BufferedOperation(TypeOfBufferedOperation.VariableUnavailable, transactionID, transactionID, variableID, typeOfTransaction, TypeOfOperation.OP_READ, 0, opTime));
                System.out.println("[Buffered] Variable x" + variableID + " is not available for read_write transaction T" + transactionID + " at this time.");
                return;
            }

            // judge if all up site that has this variable all have a write lock hold by this transaction
            boolean ifAllHaveAWriteLock = true;
            for (int siteID = 1; siteID <= DEFAULT_SITE_TOTAL_NUMBER; siteID++) {
                Site tempSite = this.sites.get(siteID);
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

            // if this transaction has already has all write locks, then it should read the uncommitted value of that variable
            if (ifAllHaveAWriteLock) {
                //This transaction has all the write locks, which means it can read now.
                ReadReturn readReturn = readCurrentValueOfVariableFromOneUpSite(variableID);
                if (readReturn == null) {
                    throw new NullPointerException();
                }
                int readValue = readReturn.getReadValue();
                int siteID = readReturn.getSiteNumber();
                System.out.println("[Success] The value of variable x" + variableID
                        + " read by Transaction T" + transactionID + " from Site " + siteID + " is " + readValue + ".");
                // do not need to require for a read lock since a write lock is already got
                Operation op = new Operation(transactionID, TypeOfOperation.OP_READ, siteID, variableID, readValue, opTime);
                // insert this operation into site transaction history
                insertIntoSiteTransactionHistory(siteID, op);
                // insert this operation into transaction history
                transaction.addToOperationHistory(op);
                return;
            }

            //judge if can have all read locks
            boolean ifExistsAnyConflictingWriteLockOnAllUpSites = findIfExistsAnyConflictingWriteLockOnAllUpSites(transactionID, variableID);
            if (!ifExistsAnyConflictingWriteLockOnAllUpSites) {
                //there is no conflicting locks, so just read value and get one variable to have a read lock

                // get the the read value and the site id where this transaction reads the variable value
                ReadReturn readReturn = readCurrentValueOfVariableFromOneUpSite(variableID);
                if (readReturn == null) {
                    throw new NullPointerException();
                }
                int readValue = readReturn.getReadValue();
                int siteID = readReturn.getSiteNumber();

                // add read lock on the variable that this transaction read from
                getOneReadLockedOnThisVariableInThisSiteByThisTransaction(siteID, transactionID, variableID);

                // Operation(int transactionID, TypeOfOperation operationType, int siteID, int variableID, int value, int time)
                Operation op = new Operation(transactionID, TypeOfOperation.OP_READ, siteID, variableID, readValue, opTime);
                insertIntoSiteTransactionHistory(siteID, op);
                transaction.addToOperationHistory(op);
                System.out.println("[Success] The value of variable x" + variableID
                        + " read by Transaction T" + transactionID + " from Site " + siteID + " is " + readValue + ".");
                return;
            }

            // Buffer this operation when there is some conflicting lock
            ConflictingBufferedQueryReturn queryReturnNew = findExistingAnyConflictingWriteLockOnAllUpSites(transactionID, variableID);
            int blockedTransactionID = queryReturnNew.getBufferedConflictingTransactionID();

            insertIntoBufferedWaitList(new BufferedOperation(TypeOfBufferedOperation.TransactionBlocked, transactionID, blockedTransactionID, variableID, typeOfTransaction, TypeOfOperation.OP_READ, 0, opTime));
            // put this wait-for relation in the wait for list
            insertIntoWaitForRelation(new WaitFor(transactionID, blockedTransactionID, getTransactionStartTime(transactionID)));
            System.out.println("[Buffered] R(T" + transactionID + ", x" + variableID + ") has to wait because it cannot acquire the read lock on that variable blocked by Transaction T" + blockedTransactionID +".");
        }
    }


    /**
     *  Put successfully executed operation into site transaction history
     */
    private void insertIntoSiteTransactionHistory(int siteID, Operation op) {
        this.SiteTransactionHistory.get(siteID).add(op);
    }


    /**
     *  Put wait for relation into wait for relation list
     */
    private void insertIntoWaitForRelation(WaitFor waitFor) {
        this.waitForList.add(waitFor);
    }


    /**
     *  Put buffered operation into operation wait list
     */
    private void insertIntoBufferedWaitList(BufferedOperation bufferedOperation) {
        this.bufferedWaitList.add(bufferedOperation);
    }


    /**
     *  Check if there exists any conflicting WRITE type buffered operation in the buffered operation list
     *  Called by Read_Write Transaction when it wants to read some variables
     */
    private ConflictingBufferedQueryReturn findExistingConflictingWritingBufferedOperation(int variableID) {
        // BufferedOperation(TypeOfBufferedOperation typeOfBufferedOperation, int transactionID, int variableID,
        //                      TypeOfTransaction typeOfTransaction, TypeOfOperation typeOfOperation, int bufferedTime)
        for (BufferedOperation BO : this.bufferedWaitList) {
            if (BO.getVariableID() == variableID && BO.getTypeOfOperation() == TypeOfOperation.OP_WRITE) {
                return new ConflictingBufferedQueryReturn(true, BO.getTransactionID());
            }
        }
        return new ConflictingBufferedQueryReturn(false, -1);
    }


    /**
     *  Called by Read_Write Transaction when it wants to read some variables and it has write lock on these variables
     */
    private ReadReturn readCurrentValueOfVariableFromOneUpSite(int variableID) {
        //  ReadReturn(int siteNumber, int readValue)
        for (int siteID = 1; siteID <= DEFAULT_SITE_TOTAL_NUMBER; siteID++) {
            Site tempSite = this.sites.get(siteID);
            if (!tempSite.getIfSiteWorking()) {
                continue;
            }
            if (!tempSite.ifHaveThisVariable(variableID)) {
                continue;
            }
            int currentValueRead = tempSite.returnThisVariableCurrentValue(variableID);
            return new ReadReturn(siteID, currentValueRead);
        }
        return null;
    }


    /**
     *  Called by Read_Write Transaction when it wants to read some variables
     *      and see if there is any conflicting write lock
     */
    private boolean findIfExistsAnyConflictingWriteLockOnAllUpSites(int transactionID, int variableID) {
        for (int siteID = 1; siteID <= DEFAULT_SITE_TOTAL_NUMBER; siteID++) {
            //System.out.println("Looking at site" + i + ":");
            Site tempSite = this.sites.get(siteID);
            if (tempSite.getIfSiteWorking() && tempSite.ifContainsVariable(variableID)) {
                //System.out.println("Entering site" + siteID + ":");
                // Get the list of all locks on the required variable
                List<LockOnVariable> lockListOnThisVariable = tempSite.getLockTableOfSite().getAllLocksOnVariable(variableID);
                for (LockOnVariable lock : lockListOnThisVariable) {
                    if (lock.getVariableID() == variableID && lock.getTransactionID() != transactionID && lock.getLockType() != TypeOfLock.Read) {
                        //System.out.println("[Failure] Transaction T" + transactionID + " is blocked by Transaction T" + lock.getTransactionID() + " on Variable x" + variableID + ".");
                        return true;
                    }
                }
            }
        }
        return false;
    }


    /**
     *  Called by Read_Write Transaction when read value of one variable and have a lock on it.
     */
    private void getOneReadLockedOnThisVariableInThisSiteByThisTransaction(int siteID, int transactionID, int variableID) {
        Site tempSite = this.sites.get(siteID);
        if (tempSite.getIfSiteWorking() && tempSite.ifContainsVariable(variableID) && tempSite.ifThisVariableIsAvailable(variableID)) {
            List<LockOnVariable> lockListOnThisVariable = tempSite.getLockTableOfSite().getAllLocksOnVariable(variableID);
            if (lockListOnThisVariable.size() == 0) {
                tempSite.getLockTableOfSite().addLock(variableID, transactionID, TypeOfLock.Read);
                System.out.println("A read lock added in site " + siteID
                        + " by Transaction T" + transactionID + " on variable x" + variableID + "!");
            } else {
                for (LockOnVariable lock : lockListOnThisVariable) {
                    if (lock.getTransactionID() == transactionID) {
                        System.out.println("A " + lock.getLockType() + " lock in site " + siteID
                                + " has already got by Transaction T" + transactionID + " on variable x" + variableID + "!");
                        return;
                    }
                }
                tempSite.getLockTableOfSite().addLock(variableID, transactionID, TypeOfLock.Read);
                System.out.println("A read lock added in site " + siteID
                        + " by Transaction T" + transactionID + " on variable x" + variableID + "!");
            }
        }
    }


    /**
     *  Called by Read_Write Transaction when it wants to read some variables
     *      and see if there is any conflicting write lock
     *      and return the query result
     *      ConflictingBufferedQueryReturn(boolean ifExistsAnyConflictingBufferedOperations, int bufferedConflictingTransactionID)
     */
    private ConflictingBufferedQueryReturn findExistingAnyConflictingWriteLockOnAllUpSites(int transactionID, int variableID) {
        for (int siteID = 1; siteID <= DEFAULT_SITE_TOTAL_NUMBER; siteID++) {
            //System.out.println("Looking at site" + i + ":");
            Site tempSite = this.sites.get(siteID);
            if (tempSite.getIfSiteWorking() && tempSite.ifContainsVariable(variableID)) {
                //System.out.println("Entering site" + i + ":");
                List<LockOnVariable> lockListOnThisVariable = tempSite.getLockTableOfSite().getAllLocksOnVariable(variableID);
                for (LockOnVariable lock : lockListOnThisVariable) {
                    if (lock.getVariableID() == variableID && lock.getTransactionID() != transactionID && lock.getLockType() != TypeOfLock.Read) {
                        System.out.println("[Failure] Transaction T" + transactionID + " is blocked by Transaction T" + lock.getTransactionID() + " on Variable x" + variableID + ".");
                        return new ConflictingBufferedQueryReturn(true, lock.getTransactionID());
                    }
                }
            }
        }
        return new ConflictingBufferedQueryReturn(false, -1);
    }


    /**
     *  Write values to the all available variable
     */
    private void writeVariableValue(int transactionID, int variableID, int value, int opTime) {

        if (!currentTransactions.containsKey(transactionID)) {
            //check if this transaction alive or not
            System.out.println("[Failure] This transaction may be aborted already or not initiated!");
            return;
        }

        Transaction transaction = currentTransactions.get(transactionID);
        if (transaction.getTransactionType() != TypeOfTransaction.Read_Write) {
            System.out.println("[Failure] Please make sure that this transaction is actually a READ_WRITE one!");
            return;
        }

        // check if there is any buffered operations left in the buffered operation list with the query on the same variable
        // ConflictingBufferedQueryReturn(boolean ifExistsAnyConflictingBufferedOperations, int bufferedConflictingTransactionID)
        ConflictingBufferedQueryReturn queryReturn = findExistingConflictingAnyBufferedOperation(variableID);

        if (queryReturn.getIfExistsAnyConflictingBufferedOperations()) {
            int waitForTransactionID = queryReturn.getBufferedConflictingTransactionID();
            insertIntoBufferedWaitList(new BufferedOperation(TypeOfBufferedOperation.TransactionBlocked, transactionID, waitForTransactionID, variableID, TypeOfTransaction.Read_Write, TypeOfOperation.OP_WRITE, value, opTime));
            // put this wait-for relation in the wait for list
            insertIntoWaitForRelation(new WaitFor(transactionID, waitForTransactionID, getTransactionStartTime(transactionID)));
            System.out.println("[Buffered] Transaction T" + transactionID + " is blocked by Transaction T" + waitForTransactionID + " on Variable x" + variableID + " in the buffered operation list.");
            return;
        }

        // if there is any up site available for writing this variable
        boolean canFindThisVariable = false;
        for (int siteID = 1; siteID <= DEFAULT_SITE_TOTAL_NUMBER; siteID++) {
            Site tempSite = this.sites.get(siteID);
            if (!tempSite.getIfSiteWorking()) {
                continue;
            }
            if (tempSite.ifContainsVariable(variableID)) {
                canFindThisVariable = true;
                break;
            }
        }
        if (!canFindThisVariable) {
            // cannot find any up sites that contains this variable
            // set previousWaitingTransactionID field to be the same as this transactionID
            //      because this transaction is blocked due to variable unavailable, not because of can acquire the lock on that variable
            //      so this field has not meaning when the TypeOfBufferedOperation is VariableUnavailable
            insertIntoBufferedWaitList(new BufferedOperation(TypeOfBufferedOperation.VariableUnavailable, transactionID, transactionID, variableID, TypeOfTransaction.Read_Write, TypeOfOperation.OP_WRITE, value, opTime));
            System.out.println("[Buffered] Variable x" + variableID + " is not available for read_write transaction T" + transactionID + " at this time.");
            return;
        }

        // judge if all up site that has this variable all have a write lock hold by this transaction
        // if does, then it can write to variables
        // otherwise, it's not the right time to write to variables
        boolean ifAllHaveAWriteLock = true;
        for (int siteID = 1; siteID <= DEFAULT_SITE_TOTAL_NUMBER; siteID++) {
            Site tempSite = this.sites.get(siteID);
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

        if (ifAllHaveAWriteLock) {
            // This transaction has all the write locks that it needs, which means it can write now.
            this.writeToAllUpSites(transactionID, variableID, value, opTime);
            int siteID = 0; //don't care
            Operation op = new Operation(transactionID, TypeOfOperation.OP_WRITE, siteID, variableID, value, opTime);
            insertIntoSiteTransactionHistory(siteID, op);
            transaction.addToOperationHistory(op);
            System.out.println("[Success] Variable x" + variableID
                    + " on all up sites has their uncommitted value to be " + value
                    + " by transaction T" + transactionID + " and it have already had all the locks.");
        } else {
            // TODO
            // TODO
            // TODO
            // TODO
            // TODO
            // TODO
            // TODO
            // boolean ifThisTransactionCanHaveWriteLockOnAllUpSites = findIfExistsConflictLockOnAllUpSites(transactionID, variableID);
            // System.out.println("T" + transactionID + " & " + "x" + variableID);
            if (!findIfExistsConflictLockOnAllUpSites(transactionID, variableID)) {
                // if there is no conflicting lock on this variable on any up sites
                getAllWriteLockedOnAllUpSitesByThisTransaction(transactionID, variableID);
                writeToAllUpSites(transactionID, variableID, value, opTime);
                int siteID = 0; //don't care
                Operation op = new Operation(transactionID, TypeOfOperation.OP_WRITE, siteID, variableID, value, opTime);
                // insertIntoSiteTransactionHistory(siteID, op);
                // This step is done is the writeToAllUpSites function
                transaction.addToOperationHistory(op);
                System.out.println("[Success] Variable x" + variableID
                        + " on all up sites has their temp uncommitted value to be " + value
                        + " by transaction T" + transactionID + " and it have already had all the locks.");
            }
            else {
                System.out.println("[Trace] !!!!!!!!!!!!!!!!!!");
                getAllWriteLockedOnLockableVariableAllUpSitesByThisTransaction(transactionID, variableID);

                //There is some conflicting lock
                ConflictingBufferedQueryReturn queryReturnNew = findExistingAnyConflictingLockOnAllUpSites(transactionID, variableID);
                int blockedTransactionID = queryReturnNew.getBufferedConflictingTransactionID();

                insertIntoBufferedWaitList(new BufferedOperation(TypeOfBufferedOperation.TransactionBlocked, transactionID, blockedTransactionID, variableID, TypeOfTransaction.Read_Write, TypeOfOperation.OP_WRITE, value, opTime));
                // put this wait-for relation in the wait for list
                insertIntoWaitForRelation(new WaitFor(transactionID, blockedTransactionID, getTransactionStartTime(transactionID)));
                System.out.println("[Buffered] W(T" + transactionID + ", x" + variableID + ", " + value + ") has to wait because it cannot acquire the write lock on that variable blocked by Transaction T" + blockedTransactionID +".");
            }
        }
    }




    /**
     *  Check if there exists any conflicting buffered operation in the buffered operation list
     *  Called by Read_Write Transaction when it wants to write value to some variables
     */
    private ConflictingBufferedQueryReturn findExistingConflictingAnyBufferedOperation(int variableID) {
        for (BufferedOperation BO : bufferedWaitList) {
            if (BO.getVariableID() == variableID) {
                return new ConflictingBufferedQueryReturn(true, BO.getTransactionID());
            }
        }
        return new ConflictingBufferedQueryReturn(false, -1);
    }


    /**
     *  Check if there exists any conflicting locks on this variable in any up sites
     *  Called by Read_Write Transaction when it wants to write value to some variables
     */
    private boolean findIfExistsConflictLockOnAllUpSites(int transactionID, int variableID) {
        //find if exists any read or write lock hold by other transaction on this variable
        for (int siteID = 1; siteID <= DEFAULT_SITE_TOTAL_NUMBER; siteID++) {
            //System.out.println("Looking at site" + i + ":");
            Site tempSite = this.sites.get(siteID);
            if (tempSite.getIfSiteWorking() && tempSite.ifContainsVariable(variableID)) {
                //System.out.println("Entering site" + siteID + ":");
                List<LockOnVariable> lockListOnThisVariable = tempSite.getLockTableOfSite().getAllLocksOnVariable(variableID);
                for (LockOnVariable lock : lockListOnThisVariable) {
                    if (lock.getVariableID() == variableID && lock.getTransactionID() != transactionID) {
                        System.out.println("[Failure] Transaction T" + transactionID + " is blocked by Transaction T" + lock.getTransactionID() + " on Variable x" + variableID + ".");
                        return true;
                    }
                }
            }
        }
        return false;
    }


    /**
     *  Get all variables get a write lock by this transaction
     */
    private void getAllWriteLockedOnLockableVariableAllUpSitesByThisTransaction(int transactionID, int variableID) {
        for (int siteID = 1; siteID <= DEFAULT_SITE_TOTAL_NUMBER; siteID++) {
            Site tempSite = this.sites.get(siteID);
            if (tempSite.getIfSiteWorking() && tempSite.ifContainsVariable(variableID)) {
                List<LockOnVariable> lockListOnThisVariable = tempSite.getLockTableOfSite().getAllLocksOnVariable(variableID);
                if (lockListOnThisVariable.size() == 0) {
                    tempSite.getLockTableOfSite().addLock(variableID, transactionID, TypeOfLock.Write);
                    System.out.println("At Site " + siteID + ", a WRITE lock on Variable x" + variableID
                            + " is added by Transaction T" + transactionID + ".");
                } else {
                    boolean canGetAWriteLock = true;
                    for (LockOnVariable lock : lockListOnThisVariable) {
                        // TODO
                        if ( lock.getTransactionID() != transactionID) {
                            System.out.println("AAAAAAAAt Site " + siteID + ", a WRITE lock on Variable x" + variableID
                                    + " cannot be added by Transaction T" + transactionID + ".");
                            canGetAWriteLock = false;
                            break;
                        }
                    }
                    if (canGetAWriteLock) {
                        LockOnVariable thisLock = lockListOnThisVariable.get(0);
                        if (thisLock.getLockType() == TypeOfLock.Read) {
                            tempSite.getLockTableOfSite().updateReadLockToWriteLock(variableID, transactionID);
                            System.out.println("At Site " + siteID + ", a READ lock on Variable x" + variableID
                                    + " is upgraded to a WRITE one by Transaction T" + transactionID + ".");
                        } else {
                            System.out.println("At Site " + siteID + ", a WRITE lock on Variable x" + variableID
                                    + " is already get by Transaction T" + transactionID + ".");
                        }
                    }
                }
            }
        }
    }


    /**
     *  Get all variables which can have a write lock on to get a write lock by this transaction
     */
    private void getAllWriteLockedOnAllUpSitesByThisTransaction(int transactionID, int variableID) {
        for (int siteID = 1; siteID <= DEFAULT_SITE_TOTAL_NUMBER; siteID++) {
            Site tempSite = this.sites.get(siteID);
            if (tempSite.getIfSiteWorking() && tempSite.ifContainsVariable(variableID)) {
                List<LockOnVariable> lockListOnThisVariable = tempSite.getLockTableOfSite().getAllLocksOnVariable(variableID);
                if (lockListOnThisVariable.size() == 0) {
                    tempSite.getLockTableOfSite().addLock(variableID, transactionID, TypeOfLock.Write);
                    System.out.println("At Site " + siteID + ", a WRITE lock on Variable x" + variableID
                            + " is added by Transaction T" + transactionID + ".");
                } else {
                    LockOnVariable thisLock = lockListOnThisVariable.get(0);
                    if (thisLock.getLockType() == TypeOfLock.Read) {
                        tempSite.getLockTableOfSite().updateReadLockToWriteLock(variableID, transactionID);
                        System.out.println("At Site " + siteID + ", a READ lock on Variable x" + variableID
                                + " is upgraded to a WRITE one by Transaction T" + transactionID + ".");
                    } else {
                        System.out.println("At Site " + siteID + ", a WRITE lock on Variable x" + variableID
                                + " is already get by Transaction T" + transactionID + ".");
                    }
                }
            }
        }
    }


    /**
     *  write value to all required variables in any up sites
     */
    private void writeToAllUpSites(int transactionID, int variableID, int value, int opTime) {
        for (int siteID = 1; siteID <= DEFAULT_SITE_TOTAL_NUMBER; siteID++) {
            if (this.sites.get(siteID).getIfSiteWorking() && this.sites.get(siteID).ifContainsVariable(variableID)
                    && this.sites.get(siteID).getLockTableOfSite().ifThisTransactionHasWriteLockInThisLockTable(transactionID)) {
                this.sites.get(siteID).writeToVariableCurrValueInThisSite(variableID, value);
                Operation op = new Operation(transactionID, TypeOfOperation.OP_WRITE, siteID, variableID, value, opTime);
                insertIntoSiteTransactionHistory(siteID, op);
            }
        }
    }


    /**
     *  Find any existing conflicting locks on any up sites
     *  Called by Read_Write Transaction when it wants to do write operations
     */
    private ConflictingBufferedQueryReturn findExistingAnyConflictingLockOnAllUpSites(int transactionID, int variableID) {
        for (int siteID = 1; siteID <= DEFAULT_SITE_TOTAL_NUMBER; siteID++) {
            //System.out.println("Looking at site" + i + ":");
            Site tempSite = this.sites.get(siteID);
            if (tempSite.getIfSiteWorking() && tempSite.ifContainsVariable(variableID)) {
                //System.out.println("Entering site" + i + ":");
                List<LockOnVariable> lockListOnThisVariable = tempSite.getLockTableOfSite().getAllLocksOnVariable(variableID);
                for (LockOnVariable lock : lockListOnThisVariable) {
                    if (lock.getVariableID() == variableID && lock.getTransactionID() != transactionID) {
                        System.out.println("[Failure] Transaction T" + transactionID + " is blocked by Transaction T"
                                + lock.getTransactionID() + " on Variable x" + variableID + " at Site " + siteID + ".");
                        return new ConflictingBufferedQueryReturn(true, lock.getTransactionID());
                    }
                }
            }
        }
        return new ConflictingBufferedQueryReturn(false, -1);
    }


    /**
     *  Gives the committed values of all copies of all variables at all sties, sorted per site
     */
    private void dump() {
        for (int siteID = 1; siteID <= DEFAULT_SITE_TOTAL_NUMBER; siteID++) {
            System.out.println("At site " + siteID + ":");
            System.out.println(sites.get(siteID).dumpOutput());//
        }
    }


    /**
     *  Gives the committed values of all copies of all variables at site i
     */
    private void dump(int index) {
        if (sites.containsKey(index)) {
            System.out.println("At site " + index + ":");
            System.out.println(sites.get(index).dumpOutput());
        } else {
            throw new IndexOutOfBoundsException();
        }
    }


    /**
     *  Gives the committed values of all copies of variable xj at all sties
     */
    private void dumpVariable(int index) {
        for (int siteID = 1; siteID <= DEFAULT_SITE_TOTAL_NUMBER; siteID++) {
            ArrayList<Variable> variableList = (ArrayList<Variable>) sites.get(siteID).getVariableList();
            for (Variable variable : variableList) {
                if (variable.getID() == index) {
                    System.out.println("At site " + siteID + ":");
                    System.out.println(variable.variableOutput());
                }
            }
        }
    }


    /**
     *  Report whether this transaction can commit or not
     */
    private void endTransaction(int transactionID) {

        System.out.println("[Report] Now analyzing whether Transaction T" + transactionID + " can commit or not.");

        if (hasAborted(transactionID)) {
            System.out.println("[Aborted] This transaction T" + transactionID + " has already been aborted!");
            return;
        }

        if (!this.currentTransactions.containsKey(transactionID)) {
            System.out.println("[Failure] No such Transaction T" + transactionID + " to end!");
            return;
        }

        Transaction transactionToBeEnded = this.currentTransactions.get(transactionID);

        // check if this transaction is blocked
        if (bufferedWaitList != null && ifExistsBufferedOperation(transactionID)) {
            System.out.println("[Blocked] This transaction T" + transactionID + " has been blocked! It cannot commit at this time.");
            return;
        }

        if (transactionToBeEnded.getTransactionType() == TypeOfTransaction.Read_Write) {
            // If the transaction to be ended is a read_write one, then start to commit transaction
            for (int siteID = 1; siteID <= DEFAULT_SITE_TOTAL_NUMBER; siteID++) {
                Site tempSite = this.sites.get(siteID);
                List<LockOnVariable> table = tempSite.getLockTableOfSite().getLockTable();
                //indexList records the position of locks in this lock table list
                List<LockOnVariable> indexList= new ArrayList<>();
                for (int j = 0; j < table.size(); j++) {
                    LockOnVariable lock = table.get(j);
//                    String typeOfLock = (lock.getLockType() == TypeOfLock.Read) ? "Read" : "Write";
//                    System.out.println("At site " + tempSite.getSiteID()
//                            + ", Transaction T" + lock.getTransactionID()
//                            + " holds a " + typeOfLock + " lock on variable x" + lock.getVariableID() + ".");

                    if (lock.getTransactionID() != transactionID) {
                        continue;
                    }

                    if (lock.getLockType() == TypeOfLock.Read) {
                        System.out.println("Starting to release read lock on variable x" + lock.getVariableID()
                                + " held by Transaction T" + lock.getTransactionID()
                                + " at Site " + tempSite.getSiteID() + "." );
                        indexList.add(lock);
                        continue;
                    }

                    if (lock.getLockType() == TypeOfLock.Write) {
                        System.out.println("Starting to release write lock on variable x" + lock.getVariableID()
                                + " held by Transaction T" + lock.getTransactionID()
                                + " at Site " + tempSite.getSiteID() + "." );
                        tempSite.CommitTheWrite(lock, time);
                        indexList.add(lock);
                    }
                }

                table.removeAll(indexList);
//                Collections.sort(indexList);
//
//                // remove locks from this lock table
//                for (int k = indexList.size() - 1; k >= 0; k--) {
//                    tempSite.ReleaseThatLock(table.get(k));
//                }

            }
        } else {
            // for read-only transaction, it can commit only when all sites it reads from are up
            // TODO
            // TODO
            // TODO
            // TODO
            // TODO
            // TODO
            List<Operation> history = transactionToBeEnded.getOperationHistory();
            boolean allSiteUp = true;
            for (Operation op : history) {
                int siteID = op.getSiteID();
                if(!this.sites.get(siteID).getIfSiteWorking()) {
                    allSiteUp = false;
                    break;
                }
            }
            if (!allSiteUp) {
                System.out.println("[Waiting] This Read-only Transaction T" + transactionID + " cannot commit at this time because there exists some sites it reads from are not recovered from failure..");
            }
        }
        // remove from wait for list
        clearAllRelatedWaitForList(transactionID);

        reportTransaction(transactionID);
        // add this transaction into committed transaction list
        addToCommittedTransaction(transactionID);
        // remove this transaction from current transaction list
        removeFromCurrentTransaction(transactionID);
        // remove this transaction records from all sites' transaction history
        removeFromAllRelatedSiteTransaction(transactionID);

        System.out.println("[Committed] This transaction T" + transactionID + " has been committed!");
    }


    /**
     *  Check whether this transaction has been aborted or not
     */
    private boolean hasAborted(int tid) {
        return abortedTransactions.contains(tid);
    }


    /**
     *  Add this transaction ID to committed transaction list
     */
    private void addToCommittedTransaction(int transactionID) {
        this.committedTransactions.add(transactionID);
    }


    /**
     *  Report all the records related to this transaction
     */
    private void reportTransaction(int transactionID) {
        Transaction transaction = this.currentTransactions.get(transactionID);
        List<Operation> listHistory = transaction.getOperationHistory();
        System.out.println("\n" + "[Report] Now reporting all the execution records of Transaction T" + transactionID + ":");
        if (listHistory.size() == 0) {
            System.out.println("[Report] This Transaction T" + transactionID + " seems to have no actions.");
            return;
        }

        for (Operation operation : listHistory) {
            // Operation(int transactionID, TypeOfOperation operationType,
            //  int siteID, int variableID, int value, int time)
            if (operation.getOperationType() == TypeOfOperation.OP_READ) {
                System.out.println("Transaction T" + transactionID
                        + " reads value of Variable x" + operation.getVariableID()
                        + " in Site " + operation.getSiteID()
                        + " and gets " + operation.getValue() + " at Time " + operation.getTime() + ".");
            } else if (operation.getOperationType() == TypeOfOperation.OP_WRITE) {
                System.out.println("Transaction T" + transactionID
                        + " writes value " + operation.getValue() + " to Variable x" + operation.getVariableID()
                        + " in all available Sites at Time " + operation.getTime() + ".");
            }
        }
        System.out.println(" ");
    }


    /**
     *  Check if there exists any buffered operations in the buffered operation list
     */
    private boolean ifExistsBufferedOperation(int transactionID) {
        if (bufferedWaitList == null) {
            return false;
        }
        for (BufferedOperation BO : bufferedWaitList) {
            if (BO.getTransactionID() == transactionID) {
                return true;
            }
        }
        return false;
    }


    /**
     *  This site is down. Start to reset this site and abort all affected transactions
     */
    private void failSite(int siteID) {
        if (this.sites.containsKey(siteID) && this.sites.get(siteID).getIfSiteWorking()) {
            System.out.println("[Down] Site " + siteID + " is now failed.");
            List<Integer> affectedTransactionList = outputAffectedTransactionList(siteID);
            this.sites.get(siteID).failThisSite();
            // export all affected transaction id to the list of transactions to be aborted
            this.toBeAbortedList.addAll(affectedTransactionList);
            // reset Site History Record
            this.SiteTransactionHistory.remove(siteID);
            this.SiteTransactionHistory.put(siteID, new ArrayList<>());
        } else {
            System.out.println("[Failure] Unable to fail this site. Maybe it is still down or not even exists!");
        }
    }


    /**
     *  Output the affected transaction list when one site fails
     */
    private List<Integer> outputAffectedTransactionList(int siteID) {

        Site tempSite = this.sites.get(siteID);
        List<LockOnVariable> lockTable = tempSite.getLockTableOfSite().getLockTable();
        Set<Integer> set = new HashSet<>();
        List<Integer> resultList = new ArrayList<>();

        // if transaction has locks on the variables in this site, it should be considered as affected
        // TODO
        // TODO
        // TODO
        // TODO
        // TODO
        // TODO
        // TODO
        // TODO
        for (LockOnVariable lock : lockTable) {
            int tID = lock.getTransactionID();
            if (!set.contains(tID)) {
                set.add(tID);
                System.out.println("Transaction T" + tID + " will be aborted!");
                resultList.add(tID);
            }
        }

        List<Operation> tempList = this.SiteTransactionHistory.get(siteID);

        // Operation(int transactionID, TypeOfOperation operationType,
        //              int siteID, int variableID, int value, int time)
        for (Operation o : tempList) {
            int tID = o.getTransactionID();
            if (ifThisTransactionIsReadOnly(tID)) {
                continue;
            }
            if (!set.contains(tID)) {
                set.add(tID);
                System.out.println("Transaction T" + tID + " should be aborted!");
                resultList.add(tID);
            }
        }
        return resultList;
    }


    /**
     *  Judge if this transaction is read-only or not
     */
    private boolean ifThisTransactionIsReadOnly(int tID) {
        return this.currentTransactions.get(tID).getTransactionType() == TypeOfTransaction.Read_Only;
    }


    /**
     *  Recover this site.
     */
    private void recoverSite(int siteID) {
        if (this.sites.containsKey(siteID) && !this.sites.get(siteID).getIfSiteWorking()) {
            this.sites.get(siteID).recoverThisSite();
        } else {
            System.out.println("[Failure] Unable to recover this site. Maybe it is still working or not even exists!");
        }
    }


    /**
     *  Abort chosen transaction
     */
    private void abort(int abortTransactionID) {
//        private Map<Integer, Transaction> currentTransactions;
//        private Set<Integer> abortedTransactions;
//        public List<BufferedOperation> bufferedWaitList;
//        private List<WaitFor> waitForList;
        clearAllRelatedBufferedWaitList(abortTransactionID);
        clearAllRelatedWaitForList(abortTransactionID);
        cancelOffTotal(abortTransactionID);
        addToAbortedTransaction(abortTransactionID);
        removeFromCurrentTransaction(abortTransactionID);
        removeFromAllRelatedSiteTransaction(abortTransactionID);
    }


    /**
     *  Clear all related buffered operations when aborting transaction
     */
    private void clearAllRelatedBufferedWaitList(int abortTransactionID) {
        //  List<BufferedOperation> bufferedWaitList;
        //  BufferedOperation(TypeOfBufferedOperation typeOfBufferedOperation,
        //                      int transactionID, int previousWaitingTransactionID,
        //                      int variableID, TypeOfTransaction typeOfTransaction,
        //                      TypeOfOperation typeOfOperation, int value, int bufferedTime)
        List<BufferedOperation> found = new ArrayList<>();
        for (BufferedOperation BO : this.bufferedWaitList) {
            if (BO.getTransactionID() == abortTransactionID) {
                found.add(BO);
            }
        }
        this.bufferedWaitList.removeAll(found);
    }


    /**
     *  Clear all related wait-for relations when aborting transaction
     */
    private void clearAllRelatedWaitForList(int abortTransactionID) {
        //  List<WaitFor> waitForList;
        //  WaitFor(int from, int to, int time)
        List<WaitFor> found = new ArrayList<>();
        for (WaitFor WF : this.waitForList) {
            if (WF.getFrom() == abortTransactionID
                    || WF.getTo() == abortTransactionID) {
                found.add(WF);
            }
        }
        this.waitForList.removeAll(found);
    }


    /**
     *  Row back aborted transactions
     */
    private void cancelOffTotal(int abortTransactionID) {
        //Transaction transaction = currentTransactions.get(abortTransactionID);
        //List<Operation> opList = transaction.getOperationHistory();
        //for (Operation o : opList) {}
        for (int siteID = 1; siteID <= DEFAULT_SITE_TOTAL_NUMBER; siteID++) {
            Site tempSite = this.sites.get(siteID);
            if (!tempSite.getIfSiteWorking()) {
                continue;
            }
            List<LockOnVariable> table = tempSite.getLockTableOfSite().getLockTable();

            List<LockOnVariable> indexList= new ArrayList<>();
            for (int j = 0; j < table.size(); j++) {
                LockOnVariable lock = table.get(j);
                String typeOfLock = (lock.getLockType() == TypeOfLock.Read) ? "Read" : "Write";
                System.out.println("At site " + tempSite.getSiteID()
                        + ", Transaction T" + lock.getTransactionID()
                        + " holds a " + typeOfLock + " lock on variable x" + lock.getVariableID() + ".");

                if (lock.getTransactionID() != abortTransactionID) {
                    continue;
                }

                if (lock.getLockType() == TypeOfLock.Read) {
                    indexList.add(lock);
                    continue;
                }

                if (lock.getLockType() == TypeOfLock.Write) {
                    System.out.println("Starting to release write lock on variable x" + lock.getVariableID()
                            + " held by Transaction T" + lock.getTransactionID() + "." );
                    //tempSite.ReverseTheWrite(lock);
                    indexList.add(lock);
                }
            }

            table.removeAll(indexList);
//            Collections.sort(indexList);
//
//            for (int k = indexList.size() - 1; k >= 0; k--) {
//                tempSite.ReleaseThatLock(table.get(k));
//            }
        }
    }


    /**
     *  Add transaction ID to aborted transaction list
     */
    private void addToAbortedTransaction(int abortTransactionID) {
        this.abortedTransactions.add(abortTransactionID);
    }


    /**
     *  Remove transaction ID from current transaction list
     */
    private void removeFromCurrentTransaction(int abortTransactionID) {
        this.currentTransactions.remove(abortTransactionID);
    }


    /**
     *  Remove all related transaction records in all the sites
     */
    private void removeFromAllRelatedSiteTransaction(int abortTransactionID) {
        for (int siteID = 1; siteID < DEFAULT_SITE_TOTAL_NUMBER; siteID++) {
            List<Operation> opList = this.SiteTransactionHistory.get(siteID);
            List<Operation> filtered = new ArrayList<>();
            for (Operation o : opList) {
                if (o.getTransactionID() == abortTransactionID) {
                    filtered.add(o);
                }
            }
            opList.removeAll(filtered);
        }
    }


    /**
     *  Return transaction start time
     */
    private int getTransactionStartTime(int transactionID) {
        if (currentTransactions.containsKey(transactionID)) {
            return currentTransactions.get(transactionID).getStartTime();
        }
        return 0;
    }


    /**Parts with the deadlock checking
     * 1. given wait-for lists
     * 2. generate matrix
     * 3. if exists cycle {
     * 4.   find one path
     * 5.   find one to abort
     * 6.   export that one to abortList
     * 7.   revise the matrix
     * 8. }
     */

    private List<Integer> deadLockRemoval(List<WaitFor> waitForList) {
        Set<Integer> set = new HashSet<>();
        List<Integer> list = new ArrayList<>();

        // find all the transaction IDs included in any wait-for relation list
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

        // sort by transaction ID
        Collections.sort(list);

        int sizeOfMat = list.size();
        int sizeOfWaitForList = waitForList.size();
        printWaitForList();
        System.out.println("[Report] Before deadlock checking, the size of this wait-for relation list is " + sizeOfWaitForList + ".");

        // create adjacency matrix for deadlock detection
        int[][] mat = new int[sizeOfMat][sizeOfMat];

        // create index for all transaction ID starting from number 0
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < sizeOfMat; i++) {
            map.put(list.get(i), i);
        }

        // fill in  1s
        for (WaitFor WF : waitForList) {
            int from = WF.getFrom();
            int to = WF.getTo();
            mat[map.get(from)][map.get(to)] = 1;
        }

        //printMatrix(mat);

        List<Integer> abortTransactionIDList = new ArrayList<>();

        Map<Boolean, Set<Integer>> cycleCheckResult = checkCycle(mat);

        while (cycleCheckResult.containsKey(true)) {
            System.out.println("A cycle is found!");
            // gets the set which contains all the vertex index in that loop
            Set<Integer> tempHashSet = cycleCheckResult.get(true);
            Set<Integer> translatedSet = translateSet(tempHashSet, list);

            // select the youngest transaction to abort
            int abortedTransactionID = -1;
            int maxTime = -1;
            for (int index : translatedSet) {
                int tempTime = getWaitForTime (waitForList, index);
                if (tempTime > maxTime) {
                    abortedTransactionID = index;
                }
            }
            //System.out.println("AbortedTransactionID is Index #" + abortedTransactionID + "!" );
            abortTransactionIDList.add(abortedTransactionID);
            //System.out.println("Setting both row and column #" + map.get(abortedTransactionID) + " to be zero!" );
            // deleting all the 1s in the matrix concerned with the aborted transaction
            reviseTheMat(mat, map.get(abortedTransactionID));
            //printMatrix(mat);
            cycleCheckResult = checkCycle(mat);
        }
        //printMatrix(mat);
        return abortTransactionIDList;
    }


    /**
     *  Print the adjacency matrix for directed graph
     */
    private void printMatrix (int[][] mat) {
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


    /**
     *  Print all wait for relations
     */
    private void printWaitForList() {
        for (WaitFor WF : this.waitForList) {
            System.out.println("\n" + "[Report] Now printing the wait-for relations:");
            System.out.println("Transaction T" + WF.getFrom() + " is waiting for Transaction T" + WF.getTo() + ".");
        }
    }


    /**
     *  Check the adjacency matrix, traverse the directed wait-for graph by DFS
     *  and return one loop(if exists)
     */
    private Map<Boolean, Set<Integer>> checkCycle(int[][] mat) {
        int size = mat.length;

        // create a list of vertices in the directed graph
        List<Vertex> V= new ArrayList<>();
        for (int i = 0; i < size; i++) {
            V.add(new Vertex());
        }

        // DFS traverse and find back edge
        for (int index = 0; index < size; index++) {
            Vertex tempVertex = V.get(index);
            // If color is white of this vertex, it means that it is not visited
            if (tempVertex.getColor() == Color.white) {
                // create a new hash set for each tree
                //System.out.println("New HashSet is created!");
                Set<Integer> tempSet = new HashSet<>();
                int tempIndex = index;
                // if the set already contains this index before, then indicates a cycle is found
                if (tempSet.contains(tempIndex)) {
                    Map<Boolean, Set<Integer>> retMap  = new HashMap<>();
                    retMap.put(true, tempSet);
                    return retMap;
                }
                tempSet.add(tempIndex);
                //System.out.println("Index #" + tempIndex + " is added.");
                tempVertex.setColor(Color.black); // set vertex state to be visited
                int[] tempRow = mat[tempIndex]; // check that row do the DFS traverse
                // return the next vertex index in the vertices list
                int nextNodeIndex = getNextIndex(tempRow);
                while (nextNodeIndex >= 0) {
                    tempIndex = nextNodeIndex;
                    if (tempSet.contains(tempIndex)) {
                        Map<Boolean, Set<Integer>> retMap  = new HashMap<>();
                        retMap.put(true, tempSet);
                        return retMap;
                    }
                    tempSet.add(tempIndex);
                    //System.out.println("Index #" + tempIndex + " is added.");
                    tempVertex = V.get(tempIndex);
                    tempVertex.setColor(Color.black);
                    tempRow = mat[tempIndex];
                    nextNodeIndex = getNextIndex(tempRow);
                }
            }
        }

        Map<Boolean, Set<Integer>> finalMap  = new HashMap<>();
        // false indicates that there is no loop found in this directed wait-for graph
        finalMap.put(false, null);
        return finalMap;
    }


    /**
     *  Check one row of adjacency matrix, and decide what is the next vertex to visit
     */
    private int getNextIndex(int[] tempRow) {
        for (int i = 0; i < tempRow.length; i++) {
            if (tempRow[i] != 0) {
                return i;
            }
        }
        return -1;
    }


    /**
     *  Translate the matrix index back to transaction ID
     */
    private Set<Integer> translateSet(Set<Integer> tempHashSet, List<Integer> list) {
        Set<Integer> result = new HashSet<>();
        for (int index : tempHashSet) {
            int translatedIndex = list.get(index);
            result.add(translatedIndex);
            System.out.println("Translated Index #" + translatedIndex + " is translated!" );
        }
        System.out.println("One translation is done!");
        return result;
    }


    /**
     *  Get buffered time for this buffered operation
     */
    private int getWaitForTime(List<WaitFor> waitForList, int translatedIndex) {
        for (WaitFor waitFor : waitForList) {
            if (waitFor.getFrom() == translatedIndex) {
                return waitFor.getTime();
            }
        }
        return -1;
    }


    /**
     *  Get buffered time for this buffered operation
     */
    private void reviseTheMat(int[][] mat, int clearID) {
        int size = mat.length;
        for (int i = 0; i < size; i++) {
            mat[i][clearID] = 0;
            mat[clearID][i] = 0;
        }
    }


    /**
     * print site transaction history per site
     */
    private void printSiteTransactionHistory() {
        for (int i = 1; i <= DEFAULT_SITE_TOTAL_NUMBER; i++) {
            List<Operation> list = SiteTransactionHistory.get(i);
            System.out.println("Now printing the operation history of Site " + i + ":");
            for (Operation o : list) {
                System.out.println("Transaction T" + o.getTransactionID() + " " + o.getOperationType()
                        + " on variable x" + o.getVariableID() + " with value " + o.getValue()
                        + " at time " + o.getTime() + ".");
            }
        }
    }


    /**
     * print site transaction history per site
     */
    private void printSiteLockTable() {
        for (int i = 1; i <= DEFAULT_SITE_TOTAL_NUMBER; i++) {
            List<LockOnVariable> list = this.sites.get(i).getLockTableOfSite().getLockTable();
            System.out.println("\n" + "Now printing the lock table of Site " + i + ":");
            for (LockOnVariable lock : list) {
                System.out.println("Transaction T" + lock.getTransactionID() + " holds a "
                        + lock.getLockType() + " lock on Variable x" + lock.getVariableID() + ".");
            }
        }
    }


    /**
     * print to be aborted transaction list
     */
    private void printToBeAbortedList() {
        System.out.println("Now printing the ToBEAbortedList:");
        for (Integer number : toBeAbortedList) {
            System.out.println("Transaction T" + number + " should be aborted.");
        }
    }

}