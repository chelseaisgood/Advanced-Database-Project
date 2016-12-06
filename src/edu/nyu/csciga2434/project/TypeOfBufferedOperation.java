package edu.nyu.csciga2434.project;

/**
 * User: Minda Fang
 * Date: 12/2/16
 * Time: 1:13 PM
 *
 * Used for indicating the type of this buffered operation, which tells why this operation is buffered
 * VariableUnavailable - buffered due to unable to find any available variable in any up sites
 * TransactionBlocked - buffered due to unable to have a lock due to transaction blocking
 */
public enum TypeOfBufferedOperation {
    VariableUnavailable,
    TransactionBlocked
}
