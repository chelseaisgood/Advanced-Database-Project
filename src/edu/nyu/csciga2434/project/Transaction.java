package edu.nyu.csciga2434.project;

import java.util.ArrayList;
import java.util.List;

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


	public Transaction(int transactionID, TypeOfTransaction type, int startTime){
		this.transactionID = transactionID;
		this.type = type;
		this.startTime = startTime;
        this.operationHistory = new ArrayList<>();
        this.locksList = new ArrayList<>();
    }

	public TypeOfTransaction getTransactionType() {
		return this.type;
	}

	public int getStartTime() {
		return startTime;
	}

    public void addToOperationHistory(Operation op) {
        this.operationHistory.add(op);
    }

    public List<LockOnVariable> getLocksList() {
        return locksList;
    }
}