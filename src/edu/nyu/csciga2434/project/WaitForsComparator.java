package edu.nyu.csciga2434.project;

import java.util.Comparator;

/**
 * User: Minda Fang
 * Date: 12/1/16
 * Time: 8:39 PM
 *
 * Used for sorting wait-for relations according to their transaction ID
 */

public class WaitForsComparator implements Comparator<WaitFor> {
    @Override
    public int compare(WaitFor w1, WaitFor w2) {
        return w1.getFrom() - w2.getFrom();
    }
}

