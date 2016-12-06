package edu.nyu.csciga2434.project;

/**
 * User: Minda Fang
 * Date: 12/1/16
 * Time: 8:22 PM
 *
 * A vertex is used for constructing a vertex in directed wait-for graph when loop detecting.
 * So it has field for its discover time in DFS traversal, its finish time in DFS traversal, its color indicating if it is already visited in one DFS traversal.
 */
public class Vertex {
    private int discoverTime;
    private int finishTime;
    private Color color;

    public Vertex() {
        this.discoverTime = -1; // -1 means this node not discovered yet
        this.finishTime = -1; // -1 means this node not finished yet
        this.color = Color.white;
    }

    public int getDiscoverTime() {
        return discoverTime;
    }

    public void setDiscoverTime(int discoverTime) {
        this.discoverTime = discoverTime;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public int getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(int finishTime) {
        this.finishTime = finishTime;
    }
}


