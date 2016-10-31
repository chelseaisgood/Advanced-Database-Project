package edu.nyu.csciga2434.project;

/**
 * User: Minda Fang
 * Date: 9/30/16
 * Time: 7:00 PM
 *
 */

public class Transaction {

	private final int transactionID;
	private final int startTime;
	private final boolean isReadOnly;


	public Transaction(int transactionID, int startTime, boolean isReadOnly){
		this.transactionID = transactionID;
		this.startTime = startTime;
		this.isReadOnly = isReadOnly;
	}
}