package edu.nyu.csciga2434.project;

/**
 * User: Minda Fang
 * Date: 10/31/16
 * Time: 2:47 PM
 *
 * Inside each lock, there is a field for transaction ID, a field for variable ID, and a field for lock type.
 */

public class LockOnVariable {

    private final int transactionID;
    private final int variableID;
    private TypeOfLock lockType;

    public LockOnVariable(int transactionID, int variableID, TypeOfLock lockType){
        this.transactionID = transactionID;
        this.variableID = variableID;
        this.lockType = lockType;
    }

    public int getTransactionID() {
        return this.transactionID;
    }

    public int getVariableID() {
        return this.variableID;
    }

    public TypeOfLock getLockType() {
        return this.lockType;
    }

    public void setLockType(TypeOfLock type) {
        this.lockType = type;
    }

    @Override
    public String toString() {
        return this.variableID + " " + this.transactionID + " " + this.lockType;
    }

}