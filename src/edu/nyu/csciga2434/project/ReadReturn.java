package edu.nyu.csciga2434.project;

/**
 * User: Minda Fang
 * Date: 12/2/16
 * Time: 1:40 PM
 */
public class ReadReturn {
    private final int siteNumber;
    private final int readValue;

    public ReadReturn(int siteNumber, int readValue) {
        this.siteNumber = siteNumber;
        this.readValue = readValue;
    }

    public int getSiteNumber() {
        return siteNumber;
    }

    public int getReadValue() {
        return readValue;
    }
}
