package org.DisneylandMap;

public class MapAttraction extends MapNode {
    private String name;
    private int attractionID, landID;

    public MapAttraction(String name, int attractionID, int landID, double x, double y) {
        super(x, y);
        this.name = name;
        this.attractionID = attractionID;
        this.landID = landID;
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
}
