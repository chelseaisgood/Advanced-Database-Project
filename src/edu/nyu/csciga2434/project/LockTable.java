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


    public boolean ifTransactionHasReadLockOnVariableInThisTable(int variableID, int transactionID, TypeOfLock type) {
        if (type == TypeOfLock.Read) {
            for (LockOnVariable lockTemp : lockTable) {
                if (lockTemp.getVariableID() == variableID
                        && lockTemp.getTransactionID() == transactionID
                        && (lockTemp.getLockType() == TypeOfLock.Read || lockTemp.getLockType() == TypeOfLock.Write)) {
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
}