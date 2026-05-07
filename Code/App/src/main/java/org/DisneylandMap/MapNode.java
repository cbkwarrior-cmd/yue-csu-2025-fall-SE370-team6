package org.DisneylandMap;

import java.util.ArrayList;

public class MapNode {
    public static final int CONNECTION_TYPE_WALKWAY = 0;
    public static final int CONNECTION_TYPE_TRAIN = 1;
    public static final int CONNECTION_TYPE_PARADE = 2;

    private double x, y;
    private ArrayList<Integer> connections;
    private ArrayList<Double> distances;
    private ArrayList<Integer> connectionTypes;

    public MapNode(double x, double y) {
        this.x = x;
        this.y = y;
        this.connections = new ArrayList<>();
        this.distances = new ArrayList<>();
        this.connectionTypes = new ArrayList<>();
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public ArrayList<Integer> getConnections() {
        return connections;
    }

    public ArrayList<Double> getDistances() {
        return distances;
    }

    public ArrayList<Integer> getConnectionTypes() {
        return connectionTypes;
    }
}
