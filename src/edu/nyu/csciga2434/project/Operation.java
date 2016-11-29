package edu.nyu.csciga2434.project;

/**
 * User: Minda Fang
 * Date: 11/29/16
 * Time: 10:28 PM
 *
 */

public class Operation {

    private final int value;
    private final int variableID;
    private final int time;
    private final int operationType;

    public Operation(int value, int variableID, int time, int operationType) {
        this.value = value;
        this.variableID = variableID;
        this.time = time;
        this.operationType = operationType;
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

    public int getOperationType() {
        return operationType;
    }
}
