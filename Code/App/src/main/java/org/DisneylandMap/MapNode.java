package src.main.java.org.DisneylandMap;

import java.util.ArrayList;

public class MapNode {
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
