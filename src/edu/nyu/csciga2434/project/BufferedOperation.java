package edu.nyu.csciga2434.project;

/**
 * User: Minda Fang
 * Date: 12/2/2016
 * Time: 1:14 PM
 */
public class BufferedOperation {
    private final TypeOfBufferedOperation typeOfBufferedOperation;
    private final int transactionID;
    private final int previousWaitingTransactionID;
    private final int variableID;
    private final TypeOfTransaction typeOfTransaction;
    private final TypeOfOperation typeOfOperation;
    private final int bufferedTime;

    public BufferedOperation(TypeOfBufferedOperation typeOfBufferedOperation, int transactionID, int previousWaitingTransactionID, int variableID, TypeOfTransaction typeOfTransaction, TypeOfOperation typeOfOperation, int bufferedTime) {
        this.typeOfBufferedOperation = typeOfBufferedOperation;
        this.transactionID = transactionID;
        this.previousWaitingTransactionID = previousWaitingTransactionID;
        this.variableID = variableID;
        this.typeOfTransaction = typeOfTransaction;
        this.typeOfOperation = typeOfOperation;
        this.bufferedTime = bufferedTime;
    }

    public TypeOfBufferedOperation getTypeOfBufferedOperation() {
        return typeOfBufferedOperation;
    }

    public int getTransactionID() {
        return transactionID;
    }

    public int getVariableID() {
        return variableID;
    }

    public TypeOfTransaction getTypeOfTransaction() {
        return typeOfTransaction;
    }

    public TypeOfOperation getTypeOfOperation() {
        return typeOfOperation;
    }

    public int getBufferedTime() {
        return bufferedTime;
    }

    public int getPreviousWaitingTransactionID() {
        return previousWaitingTransactionID;
    }
}
