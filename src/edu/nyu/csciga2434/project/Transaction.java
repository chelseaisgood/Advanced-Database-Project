package edu.nyu.csciga2434.project;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: Minda Fang
 * Date: 9/30/16
 * Time: 7:00 PM
 *
 */

public class Transaction {

	private final int transactionID;
	private final TypeOfTransaction type;
	private final int startTime;
    private List<Operation> operationHistory;
    private List<LockOnVariable> locksList;
    private Set<Integer> sitesAccessed;


	public Transaction(int transactionID, TypeOfTransaction type, int startTime){
		this.transactionID = transactionID;
		this.type = type;
		this.startTime = startTime;
        this.operationHistory = new ArrayList<>();
        this.locksList = new ArrayList<>();
        this.sitesAccessed = new HashSet<>();
    }

	public TypeOfTransaction getTransactionType() {
		return this.type;
	}

	public int getStartTime() {
		return startTime;
	}

    public List<Operation> getOperationHistory() {
        return operationHistory;
    }

    public void addToOperationHistory(Operation op) {
        this.operationHistory.add(op);
    }

    public List<LockOnVariable> getLocksList() {
        return locksList;
    }

    public Set<Integer> getSitesAccessed() {
        return sitesAccessed;
    }

    public void addLockTolocksList(int variableID, TypeOfLock type) {
        LockOnVariable tempLock = new LockOnVariable(variableID, this.transactionID, type);
        this.locksList.add(tempLock);
    }

    public boolean ifAlreadyHaveWriteLock(int variableID) {
        for (LockOnVariable lock : this.locksList) {
            if (lock.getLockType() == TypeOfLock.Write && lock.getVariableID() == variableID) {
                return true;
            }
        }
        return false;
    }

    public int getNumberOfLocksOnThisVariableByThisTransaction(TypeOfLock type, int variableID) {
        int result = 0;
        for (LockOnVariable lock : this.locksList) {
            if (lock.getVariableID() == variableID && lock.getLockType() == type) {
                result++;
            }
        }
        return result;
    }
}