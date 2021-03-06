package edu.nyu.csciga2434.project;

/**
 * User: Minda Fang
 * Date: 11/29/16
 * Time: 10:28 PM
 *
 * Used for constructing an operation recording detailed operation information
 * Inside each operation,
 * there is
 * a field for transaction ID,
 * a field for type of operation,
 * a field for site ID which records in which site this operation happens,
 * a field for variable ID, a field for read/write value,
 * a field for operation successfully executed time.
 */

public class Operation {

    private final int transactionID;
    private final TypeOfOperation operationType;
    private final int siteID;
    private final int variableID;
    private final int value;
    private final int time;

    public Operation(int transactionID, TypeOfOperation operationType, int siteID, int variableID, int value, int time) {
        this.transactionID = transactionID;
        this.operationType = operationType;
        this.siteID = siteID;
        this.variableID = variableID;
        this.value = value;
        this.time = time;
    }


    public int getValue() {
        return value;
    }

    public int getVariableID() {
        return variableID;
    }

    public int getTime() {
        return time;
    }

    public TypeOfOperation getOperationType() {
        return operationType;
    }

    public int getTransactionID() {
        return transactionID;
    }

    public int getSiteID() {
        return siteID;
    }
}
