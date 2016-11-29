package edu.nyu.csciga2434.project;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * User: Minda Fang
 * Date: 9/30/16
 * Time: 6:01 PM
 * 
 * A single transaction manager that translates read and write requests on variables to read and write requests on copies using the available copy algorithm. 
 * This transaction manager never fails.
 * If the TM requests  a read for transaction T and cannot get it due to failure, the TM should try another site. 
 * If no relevant site is available, then T must wait. 
 * This applies to read-only transactions as well which must have access to the latest version of each variable before the transaction begins. 
 * T may also have to wait for conflicting locks. 
 * While T is locked, no transaction can bypass it.
 */

public class TransactionManager {

    private static final int DEFAULT_SITE_TOTAL_NUMBER = 10;

    private Map<Integer, Site> sites;
    private Map<Integer, Transaction> currentTransactions;
    private int time;

    public TransactionManager() {
        this.sites = new HashMap<>();
        this.time = 0;
        for (int i = 1; i <= DEFAULT_SITE_TOTAL_NUMBER; i++) {
            sites.put(i, new Site(i));
        }

        currentTransactions = new HashMap<>();
    }

    public void readCommand(String commandLine) {
        //TODO
        this.time++;
        String[] operations = commandLine.split(";");
        ArrayList<String> endTransactionList = new ArrayList<>();
        for (String op : operations) {
            System.out.println(op);
            if (op.startsWith("begin(")) {
                begin(Integer.parseInt(op.substring(6, op.length() - 1)), TypeOfTransaction.Read_Write);
            } else if (op.startsWith("beginRO(")) {
                begin(Integer.parseInt(op.substring(8, op.length() - 1)), TypeOfTransaction.Read_Only);
            } else if (op.startsWith("R(")) {
                String[] t = op.substring(2, op.length() - 1).split(",");
                readVariableValue(Integer.parseInt(t[0].substring(1)), Integer.parseInt(t[1].substring(t[1].indexOf("x") + 1)));
            } else if (op.startsWith("W(")) {
                String[] t = op.substring(2, op.length() - 1).split(",");
                writeVariableValue(Integer.parseInt(t[0].substring(1)), Integer.parseInt(t[1].substring(t[1].indexOf("x") + 1)), Integer.parseInt(t[2]));
            } else if (op.startsWith("dump()")) {
                dump();
            } else if (op.startsWith("dump(x")) {
                int index = Integer.parseInt(op.substring(6, op.length() - 1));
                dumpVariable(index);
            } else if (op.startsWith("dump(")) {
                int index = Integer.parseInt(op.substring(5, op.length() - 1));
                dump(index);
            } else if (op.startsWith("end(")) {
                endTransactionList.add(op.substring(4, op.length() - 1));
            } else if (op.startsWith("fail(")) {
                failSite(Integer.parseInt(op.substring(5, op.length() - 1)));
            } else if (op.startsWith("recover(")) {
                recoverSite(Integer.parseInt(op.substring(8, op.length() - 1)));
            }
        }
        for (int i = 0; i < endTransactionList.size(); i++) {
            endTransaction(endTransactionList.get(i));
        }
    }


    private void failSite(int siteID) {
        if (this.sites.containsKey(siteID)) {
            Site s = sites.get(siteID);
            s.fail();
        } else {
            System.out.println("SUCH SITE DOES NOT EXIST (INVALID OPERATION fail(" + Integer.toString(siteID) + ")");
        }
    }

    private void recoverSite(int siteID) {
    }

    private void readVariableValue(int transactionID, int variable) {
        if (currentTransactions.containsKey(transactionID)) {
            Transaction t = currentTransactions.get(transactionID);
            if (t.getTransactionType() == TypeOfTransaction.Read_Only) {
                read(transactionID, variable, TypeOfTransaction.Read_Only);
            } else {
                read(transactionID, variable, TypeOfTransaction.Read_Write);
            }
        }
    }

    private void writeVariableValue(int transactionID, int variable, int value) {
        //TODO
    }

    private void begin(int transactionID, TypeOfTransaction typeOfTransaction) {
        if (!currentTransactions.containsKey(transactionID)) {
            boolean isReadyOnly;
            if (typeOfTransaction == TypeOfTransaction.Read_Write) {
                isReadyOnly = false;
            } else {
                isReadyOnly = true;
            }
            Transaction newTransaction = new Transaction(transactionID, isReadyOnly, time);
            currentTransactions.put(transactionID, newTransaction);
        }
    }

    private void read(int transactionID, int variable, TypeOfTransaction typeOfTransaction) {
        if (currentTransactions.containsKey(transactionID)) {
            Transaction currTransaction = currentTransactions.get(transactionID);
            if (currTransaction.getTransactionType() == TypeOfTransaction.Read_Only) {

            } else {}
        }
    }

    private void dump() {
        for (int i = 1; i <= DEFAULT_SITE_TOTAL_NUMBER; i++) {
            System.out.println("At site " + i + ":");
            System.out.println(sites.get(i).dumpOutput());//
        }
    }

    private void dump(int index) {
        if (sites.containsKey(index)) {
            System.out.println("At site " + index + ":");
            System.out.print(sites.get(index).dumpOutput());
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    private void dumpVariable(int index) {
        for (int i = 1; i <= DEFAULT_SITE_TOTAL_NUMBER; i++) {
            ArrayList<Variable> variableList = (ArrayList<Variable>) sites.get(i).getVariableList();
            for (int j = 0; j < variableList.size(); j++) {
                if (variableList.get(j).getID() == index) {
                    System.out.println("At site " + i + ":");
                    System.out.println(variableList.get(j).variableOutput());
                }
            }
        }
    }






}