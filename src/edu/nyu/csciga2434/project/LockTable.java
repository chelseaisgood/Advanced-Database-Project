package edu.nyu.csciga2434.project;

import java.util.*;

/**
 * User: Minda Fang
 * Date: 10/31/16
 * Time: 2:00 PM
 *
 */

public class LockTable {

    private List<LockOnVariable> lockTable;

    public LockTable(){
        lockTable = new ArrayList<>();
    }

    public List<LockOnVariable> getLockTable() {
        return this.lockTable;
    }


    /**
     * Judge if transaction has this type of lock on this variable by checking the lock table
     */
    public boolean ifTransactionHasLockOnVariableInThisTable(int variableID, int transactionID, TypeOfLock type) {
        for (LockOnVariable lockTemp : this.lockTable) {
            if (lockTemp.getVariableID() == variableID
                    && lockTemp.getTransactionID() == transactionID
                    && lockTemp.getLockType() == type) {
                return true;
            }
        }
        return false;
    }

//    public boolean ifCanHaveReadLockOnVariable(int variableID, int transactionID) {
//        List<LockOnVariable> locks = getAllLocksOnVariable(variableID);
//        if (locks.size() == 0) {
//            return true;
//        }
//        boolean hasWriteLock = false;
//        for (LockOnVariable lockTemp : locks) {
//            if (lockTemp.getLockType() == TypeOfLock.Write && lockTemp.getTransactionID() != transactionID) {
//                //Other transaction has a write lock on that variable,
//                // which means that this transaction could not require read lock on that variable.
//                hasWriteLock = true;
//            }
//        }
//
//        if (hasWriteLock) {
//            return false;
//        } else {
//            return true;
//        }
//    }


    /**
     * Get the all the locks on requested variable
     */
    public List<LockOnVariable> getAllLocksOnVariable(int variableID) {
        List<LockOnVariable> result = new ArrayList<>();
        for (LockOnVariable lock : this.lockTable) {
            if (lock.getVariableID() == variableID) {
                result.add(lock);
            }
        }
        return result;
    }


    /**
     * Add a specific type of lock on one variable by one transaction
     */
    public void addLock(int variableID, int transactionID, TypeOfLock lockType) {
        LockOnVariable tempLock = new LockOnVariable(transactionID, variableID, lockType);
        //System.out.println(tempLock.getLockType() +"+"+ tempLock.getTransactionID()+"+"+tempLock.getVariableID());
        this.lockTable.add(tempLock);
    }


    /**
     * Return true if this transaction has a write lock on that variable
     */
    public boolean ifThisTransactionHasWriteLockInThisLockTable(int transactionID) {
        for (LockOnVariable lock : this.lockTable) {
            if (lock.getTransactionID() == transactionID) {
                return true;
            }
        }
        return false;
    }


    /**
     * Upgrade the read lock on that variable to write one
     */
    public void updateReadLockToWriteLock(int variableID, int transactionID) {
        LockOnVariable lock = getAllLocksOnVariable(variableID).get(0);
        lock.setLockType(TypeOfLock.Write);
    }


    /**
     * Delete a lock in this lockable
     */
    public void deleteThisLock(LockOnVariable lock) {
        int index = Integer.MIN_VALUE;
        for (int i = 0; i < lockTable.size(); i++) {
            LockOnVariable thisLock = lockTable.get(i);
            if (thisLock.getVariableID() == lock.getVariableID()
                    && thisLock.getTransactionID() == lock.getTransactionID()
                    && thisLock.getLockType() == lock.getLockType()) {
                index = i;
                String typeOfLock = (lock.getLockType() == TypeOfLock.Read) ? "READ" : "WRITE";
                System.out.println("[Success] The " + typeOfLock + " lock on variable x" + lock.getVariableID()
                        + " held by Transaction T" + lock.getTransactionID() + " is removed.");
                break;
            }
        }
        if (index >= 0) {
            lockTable.remove(index);
        }
    }
}