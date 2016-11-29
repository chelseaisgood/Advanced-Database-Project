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

    public boolean getIfSiteWorking() {
        return ifSiteWorking;
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

    public void fail() {
        System.out.println("Setting ifSiteWorking state of this site " + this.id + " to be false");
        this.ifSiteWorking = false;
        System.out.println("Now we have to set the currVal of all variable to val");
        for (int i = 0; i < this.lockTableOfSite.lockTable.size(); i++) {
            getVariableAndID(this.lockTableOfSite.lockTable.get(i).getVariableID()).setCurrValue(
                    getVariableAndID(this.lockTableOfSite.lockTable.get(i).getVariableID()).getValue());
        }
        System.out.println("Deleting all entries from lock table of site " + this.id);
        System.out.println("Size of locktable is " + this.lockTableOfSite.lockTable.size());
        this.lockTableOfSite.lockTable.clear();
        System.out.println("Size of locktable becomes " + this.lockTableOfSite.lockTable.size());
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

}