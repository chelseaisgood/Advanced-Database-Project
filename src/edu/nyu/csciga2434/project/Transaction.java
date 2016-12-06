package edu.nyu.csciga2434.project;

import java.util.ArrayList;
import java.util.List;

/**
 * User: Minda Fang
 * Date: 9/30/16
 * Time: 7:00 PM
 *
 * Inside each transaction,
 * there is a field for transaction ID,
 * a field for transaction Type,
 * a field for transaction start-time,
 * and a field for a list of this transaction’s operation history.
 */

public class Transaction {

	private final int transactionID;
	private final TypeOfTransaction type;
	private final int startTime;
    private List<Operation> operationHistory;


	public Transaction(int transactionID, TypeOfTransaction type, int startTime){
		this.transactionID = transactionID;
		this.type = type;
		this.startTime = startTime;
        this.operationHistory = new ArrayList<>();
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


}