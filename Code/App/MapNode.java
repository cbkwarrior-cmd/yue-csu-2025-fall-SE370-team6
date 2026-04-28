import java.util.ArrayList;

public class MapNode {
    private int attractionID;
    private int landID;
    private double x, y;
    private String name;
    private ArrayList<Integer> connections;
    private ArrayList<Double> distances;
    private ArrayList<Integer> connectionTypes;

    public MapNode(String name, int attractionID, int landID, double x, double y) {
        this.name = name;
        this.attractionID = attractionID;
        this.landID = landID;
        this.x = x;
        this.y = y;
        this.connections = new ArrayList<>();
        this.distances = new ArrayList<>();
        this.connectionTypes = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public int getAttractionID() {
        return attractionID;
    }

    public int getLandID() {
        return landID;
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
