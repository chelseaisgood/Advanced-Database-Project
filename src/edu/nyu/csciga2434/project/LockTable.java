package edu.nyu.csciga2434.project;

import java.util.*;

/**
 * User: Minda Fang
 * Date: 10/31/16
 * Time: 2:00 PM
 *
 */

public class LockTable {

    public List<LockOnVariable> lockTable;

    public LockTable(){
        lockTable = new ArrayList<>();
    }


    public boolean ifTransactionHasLockOnVariableInThisTable(int variableID, int transactionID, TypeOfLock type) {
        //TODO
        if (type == TypeOfLock.Read) {
            for (LockOnVariable lockTemp : lockTable) {
                if (lockTemp.getVariableID() == variableID
                        && lockTemp.getTransactionID() == transactionID
                        && lockTemp.getLockType() == TypeOfLock.Read ) {
                    return true;
                }
            }
        } else if (type == TypeOfLock.Write) {
            for (LockOnVariable lockTemp : lockTable) {
                if (lockTemp.getVariableID() == variableID
                        && lockTemp.getTransactionID() == transactionID
                        && lockTemp.getLockType() == type) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean ifCanHaveReadLockOnVariable(int variableID, int transactionID) {
        List<LockOnVariable> locks = getAllLocksOnVariable(variableID);
        if (locks.size() == 0) {
            return true;
        }
        boolean hasWriteLock = false;
        for (LockOnVariable lockTemp : locks) {
            if (lockTemp.getLockType() == TypeOfLock.Write && lockTemp.getTransactionID() != transactionID) {
                //Other transaction has a write lock on that variable,
                // which means that this transaction could not require read lock on that variable.
                hasWriteLock = true;
            }
        }

        if (hasWriteLock) {
            return false;
        } else {
            return true;
        }
    }

    public List<LockOnVariable> getAllLocksOnVariable(int variableID) {
        List<LockOnVariable> result = new ArrayList<>();
        for (LockOnVariable lock : lockTable) {
            if (lock.getVariableID() == variableID) {
                result.add(lock);
            }
        }
        return result;
    }

    public void addLock(int variableID, int transactionID, TypeOfLock lockType) {
        LockOnVariable tempLock = new LockOnVariable(variableID, transactionID, lockType);
        this.lockTable.add(tempLock);
    }

    public boolean ifThisTransactionHasWriteLockInThisLockTable(int transactionID) {
        for (LockOnVariable lock : this.lockTable) {
            if (lock.getTransactionID() == transactionID) {
                return true;
            }
        }
        return false;
    }

    public void updateReadLockToWriteLock(int variableID, int transactionID) {
        LockOnVariable lock = getAllLocksOnVariable(variableID).get(0);
        lock.setLockType(TypeOfLock.Write);
    }
}