package edu.nyu.csciga2434.project;

/**
 * User: Minda Fang
 * Date: 12/2/16
 * Time: 2:01 PM
 */
public class ConflictingBufferedQueryReturn {
    private final boolean ifExistsAnyConflictingBufferedOperations;
    private final int bufferedConflictingTransactionID;

    public ConflictingBufferedQueryReturn(boolean ifExistsAnyConflictingBufferedOperations, int bufferedConflictingTransactionID) {
        this.ifExistsAnyConflictingBufferedOperations = ifExistsAnyConflictingBufferedOperations;
        this.bufferedConflictingTransactionID = bufferedConflictingTransactionID;
    }

    public boolean getIfExistsAnyConflictingBufferedOperations() {
        return ifExistsAnyConflictingBufferedOperations;
    }

    public int getBufferedConflictingTransactionID() {
        return bufferedConflictingTransactionID;
    }
}
