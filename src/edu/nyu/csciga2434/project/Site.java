package edu.nyu.csciga2434.project;

import java.util.*;

/**
 * User: Minda Fang
 * Date: 10/31/16
 * Time: 3:14 PM
 *
 * There are total 10 sites, numbered 1 to 10.
 * Every site has a lock table and when the site fails, the lock table is erased.
 */

public class Site {

    private static final int DEFAULT_SITE_TOTAL_NUMBER = 10;

    private final int id;
    private List<Variable> variableList;
    private LockTable lockTable;
    private boolean ifSiteWorking;

    public Site(int id){
        this.id = id;
        this.variableList = new ArrayList<>();
        this.lockTable = new LockTable();
        this.ifSiteWorking = true;
        BuildingSite();
    }

    private void BuildingSite(){
        for (int i = 1; i <= DEFAULT_SITE_TOTAL_NUMBER; i++){
            if(i % 2 == 0){
                this.variableList.add(new Variable(i));
            }else{
                if(this.id == (1 + i % 10)) {
                    this.variableList.add(new Variable(i));
                }
            }
        }
    }
}