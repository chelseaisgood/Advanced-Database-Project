package edu.nyu.csciga2434.project;

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

    public Variable(int id){
        this.id = id;
        this.value = id * 10;
    }

    public int getID(){
        return this.id;
    }

    public int getValue(){
        return this.value;
    }

    public String variableOutput(){
        return "x"+this.id+" has value of "+this.value+".\n";
    }

    public void setCurrValue(int value) {
        //TODO
    }
}