package edu.nyu.csciga2434.project;

import java.util.ArrayList;
import java.util.List;

/**
 * User: Minda Fang
 * Date: 10/30/16
 * Time: 12:00 PM
 *
 * Each variable xi is initialized to the value 10*i
 */

public class Variable {

    private final int id;
    private int value;
    private boolean availableForReading;
    private List<VariableHistory> variableHistoryList;
    private int currValue;
    private boolean hasCopy;

    public boolean isAvailableForReading() {
        return availableForReading;
    }

    public void setAvailableForReading(boolean availableForReading) {
        this.availableForReading = availableForReading;
    }

    public List<VariableHistory> getVariableHistoryList() {
        return this.variableHistoryList;
    }

    public Variable(int id){
        this.id = id;
        this.value = id * 10;
        this.availableForReading = true;
        this.variableHistoryList = new ArrayList<>();
        variableHistoryList.add(new VariableHistory(this.value, 0));
        this.hasCopy = id % 2 == 0;
        setCurrValue(this.value);
    }

    public int getID(){
        return this.id;
    }

    public int getValue(){
        return this.value;
    }

    public void setValue(int value){
        this.value = value;
    }

    public String variableOutput(){
        return "x"+this.id+" has value of "+this.value+".\n";
    }

    public void setCurrValue(int value) {
        this.currValue = value;
    }

    public int getCurrValue() {
        return currValue;
    }

    public boolean hasCopy() {
        return hasCopy;
    }
}