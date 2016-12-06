package edu.nyu.csciga2434.project;

import java.util.Comparator;

/**
 * User: Minda Fang
 * Date: 12/2/16
 * Time: 5:49 PM
 *
 * Used for sorting buffered operations according to their buffered time
 */
public class BufferedOperationComparator implements Comparator<BufferedOperation> {
    @Override
    public int compare(BufferedOperation bo1, BufferedOperation bo2) {
        return bo1.getBufferedTime() - bo2.getBufferedTime();
    }
}