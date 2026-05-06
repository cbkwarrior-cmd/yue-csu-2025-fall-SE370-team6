package org.DisneylandMap;

public class MapAttraction extends MapNode {
    public static final int CONNECTION_TYPE_WALKWAY = 0;
    public static final int CONNECTION_TYPE_TRAIN = 1;
    public static final int CONNECTION_TYPE_PARADE = 2;

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
