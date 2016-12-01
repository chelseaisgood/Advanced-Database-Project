package edu.nyu.csciga2434.project;

import java.util.*;

/**
 * User: Minda Fang
 * Date: 10/31/16
 * Time: 3:14 PM
 *
 * There are total 10 sites, numbered 1 to 10.
 * Odd indexed variable are at one site each(i.e. 1+ index # mod 10). Even indexed variables are at all sites.
 * Every site has a lock table and when the site fails, the lock table is erased.
 */

public class Site {

    private static final int DEFAULT_VARIABLE_TOTAL_NUMBER = 20;

    private final int id;
    private List<Variable> variableList;
    private LockTable lockTableOfSite;
    private boolean ifSiteWorking;

    public int getSiteID () {
        return this.id;
    }

    public boolean getIfSiteWorking() {
        return ifSiteWorking;
    }

    public List<Variable> getALLVariables() {
        return this.variableList;
    }

    public LockTable getLockTableOfSite () {
        return this.lockTableOfSite;
    }

    public Site(int id){
        this.id = id;
        this.variableList = new ArrayList<>();
        this.lockTableOfSite = new LockTable();
        BuildSite();
        this.ifSiteWorking = true;
    }

    private void BuildSite(){
        // call BuildSite() when the site is first set and include all the variables that should be initially held by this site.
        for (int i = 1; i <= DEFAULT_VARIABLE_TOTAL_NUMBER; i++){
            if(i % 2 == 0){
                this.variableList.add(new Variable(i));
            }else{
                if(this.id == (1 + i % 10)) {
                    this.variableList.add(new Variable(i));
                }
            }
        }
    }

    public String dumpOutput() {
        StringBuffer dumpOutput = new StringBuffer();
        for (int i = 0; i < variableList.size(); i++) {
            dumpOutput.append("x").append(variableList.get(i).getID()).append(" has value of ").append(
                    variableList.get(i).getValue()).append(".\n");
        }
        return dumpOutput.toString();
    }

    public List<Variable> getVariableList() {
        return this.variableList;
    }


    public Variable getVariableAndID(int variableID) {
        ArrayList<Variable> list;
        list = (ArrayList<Variable>) this.variableList;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getID() == variableID) {
                return list.get(i);
            }
        }
        return null;
    }


    public boolean ifContainsVariable(int variableID) {
        List<Variable> list = this.getVariableList();
        for (Variable var : list) {
            if (var.getID() == variableID) {
                return true;
            }
        }
        return false;
    }

    public boolean ifThisVariableIsAvailable(int variableID) {
        List<Variable> list = this.getVariableList();
        for (Variable var : list) {
            if (var.getID() == variableID && var.isAvailableForReading()) {
                return true;
            }
        }
        return false;
    }

    public void writeToVariableCurrValueInThisSite(int variableID, int value) {
        List<Variable> list = this.getALLVariables();
        for (Variable var : list) {
            if (var.getID() == variableID) {
                var.setCurrValue(value);
                System.out.println("[Success] Variable x" + variableID + " at Site " + this.id + " has temporary uncommitted value: " + value + ".");
                return;
            }
        }
    }

    public boolean ifHaveThisVariable(int variableID) {
        for(Variable var : this.variableList) {
            if (var.getID() == variableID) {
                return true;
            }
        }
        return false;
    }

    public int returnThisVariableValue(int variableID) {
        int result = 0;
        for(Variable var : this.variableList) {
            if (var.getID() == variableID) {
                return var.getValue();
            }
        }
        return result;
    }


    public void ReleaseThatLock(LockOnVariable lock) {
        lockTableOfSite.delectThisLock(lock);
        return;
    }

    public void CommitTheWrite(LockOnVariable lock) {
        if ( !ifHaveThisVariable(lock.getVariableID())) {
            System.out.println("[Failure] Cannot find this variable x" + lock.getVariableID() + " in this site " + id + ".");
            return;
        }
        int tempVariableID = lock.getVariableID();
        System.out.println("Now this variable to be updated is x" + tempVariableID);
        for (Variable var : variableList) {
            if (var.getID() == tempVariableID) {
                int valueNew = var.getCurrValue();
                var.setValue(valueNew);
                System.out.println("Now this value is " + var.getValue());
                return;
            }
        }
        return;
    }

    public void fail() {
        System.out.println("This site " + this.getSiteID() + " is down");
        this.ifSiteWorking = false;
        // erase the lock table

        //System.out.println("Deleting all entries from lock table of site " + this.getSiteID());
        //System.out.println("Size of locktable is " + this.lockTableOfSite.lockTable.size());
        this.lockTableOfSite.lockTable = new ArrayList<>();
        //System.out.println("Size of locktable becomes " + this.lockTableOfSite.lockTable.size());
    }

    public void recoverThisSite() {
        System.out.println("This site " + this.getSiteID() + " is recovered.");
        this.ifSiteWorking = true;
        //Setting only non-replicated variables as available to read
        //Replicated variables should not be available to read
        List<Variable> varList = this.getALLVariables();

        for (Variable var : varList) {
            if (var.hasCopy()) {
                var.setAvailableForReading(false);
            } else {
                var.setAvailableForReading(true);
            }
        }
    }
}