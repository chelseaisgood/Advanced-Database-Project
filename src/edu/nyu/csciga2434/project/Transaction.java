package edu.nyu.csciga2434.project;

/**
 * User: Minda Fang
 * Date: 9/30/16
 * Time: 7:00 PM
 *
 */

public class Transaction {

	private final int transactionID;
	private final boolean isReadOnly;
	private final int startTime;


	public Transaction(int transactionID, boolean isReadOnly, int startTime){
		this.transactionID = transactionID;
		this.isReadOnly = isReadOnly;
		this.startTime = startTime;
	}

	public TypeOfTransaction getTransactionType() {
		return (isReadOnly ? TypeOfTransaction.Read_Only : TypeOfTransaction.Read_Write);
	}
}