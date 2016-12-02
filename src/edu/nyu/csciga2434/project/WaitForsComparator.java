package edu.nyu.csciga2434.project;

import java.util.Comparator;

/**
 * User: Minda Fang
 * Date: 12/1/16
 * Time: 8:39 PM
 */

public class WaitForsComparator implements Comparator<WaitFor> {
    public int compare(WaitFor w1, WaitFor w2) {
        return w1.getFrom() - w2.getFrom();
    }
}

