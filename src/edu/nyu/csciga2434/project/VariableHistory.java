package edu.nyu.csciga2434.project;

/**
 * User: Minda Fang
 * Date: 11/29/16
 * Time: 3:00 PM
 *
 */
public class VariableHistory {

    private int value;
    private int time;


    public VariableHistory(int value, int time) {
        this.value = value;
        this.time = time;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }
    
}
