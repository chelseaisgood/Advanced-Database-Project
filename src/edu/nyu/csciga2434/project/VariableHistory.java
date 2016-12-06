package edu.nyu.csciga2434.project;

/**
 * User: Minda Fang
 * Date: 11/29/16
 * Time: 3:00 PM
 *
 * Inside each variable history,there is a field for the value, and time field for recording the committed time of this value.
 */
public class VariableHistory {

    private final int value;
    private final int time;


    public VariableHistory(int value, int time) {
        this.value = value;
        this.time = time;
    }

    public int getValue() {
        return value;
    }

    public int getTime() {
        return time;
    }

}
