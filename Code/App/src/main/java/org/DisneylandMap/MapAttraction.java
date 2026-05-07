package org.DisneylandMap;

public class MapAttraction extends MapNode {
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
}
