package src.main.java.org.DisneylandMap;

public class MapAttraction extends MapNode {
    // If a node stored in the nodes file has an attraction ID of ATTRACTION_ID_INVALID, then it is not an attraction.
    // If it is < ATTRACTION_ID_INVALID, then it is a train station.
    public static final int ATTRACTION_ID_INVALID = -1;

    private String name;
    private int attractionID, landID;
    private int closestTrainID;

    public MapAttraction(String name, int attractionID, int closestTrainID, int landID, double x, double y) {
        super(x, y);
        this.name = name;
        this.attractionID = attractionID;
        this.landID = landID;
        this.closestTrainID = closestTrainID;
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

    public int getClosestTrainID() {
        return closestTrainID;
    }

    public boolean isTrainStation() {
        return attractionID < ATTRACTION_ID_INVALID && attractionID >= ATTRACTION_ID_INVALID - Map.NUM_TRAIN_STATIONS;
    }

}
