package edu.nyu.csciga2434.project;

/**
 * User: Minda Fang
 * Date: 12/01/16
 * Time: 7:53 PM
 */
public class WaitFor {
    private final int from;
    private final int to;
    private final int time;

    public WaitFor(int from, int to, int time) {
        this.from = from;
        this.to = to;
        this.time = time;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    public int getTime() {
        return time;
    }
}


