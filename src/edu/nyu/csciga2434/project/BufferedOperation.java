package edu.nyu.csciga2434.project;

/**
 * User: Minda Fang
 * Date: 12/2/16
 * Time: 1:14 PM
 *
 * Used for constructing an buffered operation
 *
 * Inside each operation,
 * there is
 * a field for type of this buffered operation(blocked by transaction or variable unavailable),
 * a field for transaction ID,
 * a field for the waiting for transaction’s ID,
 * a field for variable ID,
 * a field for the type of this transaction,
 * a field for the type of this buffered operation,
 * a field for read/write value,
 * a field for this operation buffered time.
 */
public class BufferedOperation {
    private final TypeOfBufferedOperation typeOfBufferedOperation;
    private final int transactionID;
    private final int previousWaitingTransactionID;
    private final int variableID;
    private final TypeOfTransaction typeOfTransaction;
    private final TypeOfOperation typeOfOperation;
    private final int value;
    private final int bufferedTime;

    public BufferedOperation(TypeOfBufferedOperation typeOfBufferedOperation, int transactionID, int previousWaitingTransactionID, int variableID, TypeOfTransaction typeOfTransaction, TypeOfOperation typeOfOperation, int value, int bufferedTime) {
        this.typeOfBufferedOperation = typeOfBufferedOperation;
        this.transactionID = transactionID;
        this.previousWaitingTransactionID = previousWaitingTransactionID;
        this.variableID = variableID;
        this.typeOfTransaction = typeOfTransaction;
        this.typeOfOperation = typeOfOperation;
        this.value = value;
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

    public int getValue() {
        return value;
    }
}
