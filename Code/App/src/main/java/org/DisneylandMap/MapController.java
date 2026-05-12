package src.main.java.org.DisneylandMap;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.PriorityQueue;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;

public class MapController {
    // Error codes for getWaitTime
    private static final int WAIT_TIME_CLOSED = -1;
    private static final int WAIT_TIME_FAILED = -2;

    private final String MAP_IMAGE_PATH = "map.jpeg";
    private final String MAP_NODES_PATH = "nodes.txt";

    // Average speeds sourced from Google
    private final double STEPS_PER_METER = 1.31;
    private final double STEPS_PER_SECOND = 1.5;

    // Parade times sourced from the Disneyland website
    private final LocalTime PARADE_WEEKDAY_START = LocalTime.of(20, 30);
    private final LocalTime PARADE_WEEKDAY_END = LocalTime.of(21, 10);

    private final LocalTime PARADE_WEEKEND_START = LocalTime.of(22, 30);
    private final LocalTime PARADE_WEEKEND_END = LocalTime.of(23, 10);

    // Corresponds to the only train station entry in QueueTimes
    private final int TRAIN_STATION_LAND_ID = 113;
    private final int TRAIN_STATION_ATTRACTION_ID = 674;

    // Member objects
    private IQueueTime adaptor;
    private Map map;

    private static class Route {
        int eta;
        int distance;
        ArrayList<Point.Double> path = new ArrayList<>();

        boolean exists() {
            return !path.isEmpty();
        }
    }

    private Route currentRoute = new Route();
    private boolean preferTrains;

    // For transforming the on-screen map, nodes, and path
    private double cameraX = 0, cameraY = 0;
    private double mapScale = 1.0;

    private int mouseX, mouseY;
    private double dragXStart, dragYStart;
    private boolean dragging = false;

    // UI state corresponds to each stage of the route-creation pipeline.
    // Ties into visuals as well.
    private enum UIState {
        NAVIGATE,
        SELECT_POINT_B,
        SHOW_PATH,
    }
    private UIState uiState = UIState.NAVIGATE;
    private int highlightedAttractionID;

    public MapController(MapView view) {
        this.adaptor = new QueueTimesAdaptor();
        this.map = new Map(MAP_IMAGE_PATH, MAP_NODES_PATH);
        unhighlightAttraction(view);
    }

    // Helper methods for conversions between on-screen pixel coordinates and normalized "map space" which the nodes' coordinates are in.
    public int worldToPixelX(double x) {
        return MapView.MAP_X + (int)((x + .5 - cameraX) * MapView.MAP_WIDTH * mapScale);
    }

    public int worldToPixelY(double y) {
        return MapView.MAP_Y + (int)((y + .5 - cameraY) * MapView.MAP_HEIGHT * mapScale);
    }

    private double pixelToWorldX(int x) {
        return cameraX + ((double)(x - MapView.MAP_X) / MapView.MAP_WIDTH - .5) / mapScale;
    }

    private double pixelToWorldY(int y) {
        return cameraY + ((double)(y - MapView.MAP_Y) / MapView.MAP_HEIGHT - .5) / mapScale;
    }

    // Selects attraction node, displays information on information panel, and updates UI state
    private void highlightAttraction(int id, MapView view) {
        this.highlightedAttractionID = id;

        try {
            MapAttraction attraction = map.getAttractionFromID(highlightedAttractionID);

            String info = "" + attraction.getName() + ":\n";
            int waitTimeMinutes = getAttractionWaitTime(attraction);

            if(waitTimeMinutes == WAIT_TIME_FAILED) {
                info += "Error: Failed to get wait time";
            }
            else if(waitTimeMinutes == WAIT_TIME_CLOSED) {
                info += "This attraction is closed.";
            }
            else {
                info += "Wait Time: " + waitTimeMinutes + " Minutes.";
            }

            view.updateAttractionInfo(info);
        } catch(IOException | InterruptedException e) {
            e.printStackTrace();
        }

        setUiState(uiState, view);
    }

    public void unhighlightAttraction(MapView view) {
        this.highlightedAttractionID = MapAttraction.ATTRACTION_ID_INVALID;

        view.updateAttractionInfo("Press Node to View Attraction Info");
        setUiState(uiState, view);
    }

    public interface AttractionEvent {
        void execute(int pixelX, int pixelY, int id, String name, boolean highlighted);
    }

    // Helper function to iterate through each attraction and operate with additional data, such as pixel coordinates
    public void forEachAttraction(MapView view, AttractionEvent e) {
        map.forEachAttraction((id, attraction) -> {

            int x = worldToPixelX(attraction.getX());
            int y = worldToPixelY(attraction.getY());

            if(x >= MapView.MAP_X - MapView.ATTRACTION_DIAMETER
                && x < MapView.MAP_X + MapView.MAP_WIDTH + MapView.ATTRACTION_DIAMETER
                && y >= MapView.MAP_Y - MapView.ATTRACTION_DIAMETER
                && y < MapView.MAP_Y + MapView.MAP_HEIGHT + MapView.ATTRACTION_DIAMETER
            ) {
                e.execute(x, y, id, attraction.getName(), id == highlightedAttractionID);
            }
        });
    }

    // Returns string which details the ETA of currentRoute in a human-readable format
    // Will be in seconds if < 1 minute, minutes if < 1 hour, or minutes + hours if >= 1 hour
    private String formatETA() {
        String etaText = "";
        int etaMinutes = currentRoute.eta / 60;

        if(etaMinutes == 0) {
            etaText = "" + currentRoute.eta + " sec" + (currentRoute.eta == 1 ? "" : "s");
        }
        else {
            int etaHours = etaMinutes / 60;

            if(etaHours == 0) {
                etaText = "" + etaMinutes + " min" + (etaMinutes == 1 ? "" : "s");
            }
            else {
                etaText = "" + etaHours + " hr" + (etaHours == 1 ? "" : "s") + " " + etaText;
            }
        }

        return etaText;
    }

    // Updates uiState alongside visuals for UI elements, in one function.
    // Uses formatETA() when the path is visible.
    private void setUiState(UIState newState, MapView view) {
        this.uiState = newState;

        switch(uiState) {
            case NAVIGATE -> {
                if(highlightedAttractionID == MapAttraction.ATTRACTION_ID_INVALID) {
                    view.updateRouteButton(MapView.GREY_RGB, Color.BLACK, "Requires Start Point to Route");
                }
                else {
                    view.updateRouteButton(MapView.GREEN_RGB, Color.WHITE, "Begin Routing");
                }
                view.updateRouteLabels("N/A", "N/A");
            }
            case SELECT_POINT_B -> {
                view.updateRouteButton(Color.BLACK, Color.WHITE, "Cancel");
                view.updateRouteLabels("N/A", "N/A");
            }
            case SHOW_PATH -> {
                view.updateRouteButton(MapView.BLUE_RGB, Color.WHITE, "Clear Path");

                String etaText = "Impossible route";
                String distanceText = "Impossible route";

                if(currentRoute.exists()) {
                    etaText = formatETA();
                    distanceText = "" + currentRoute.distance + " steps";
                }

                view.updateRouteLabels(etaText, distanceText);
            }
        }
    }

    // Behavior for MapView's route button can modify UI state.
    public void handleRouteButton(MapView view) {
        switch(uiState) {
            case NAVIGATE -> {
                if(highlightedAttractionID != MapAttraction.ATTRACTION_ID_INVALID) {
                    setUiState(UIState.SELECT_POINT_B, view);
                }
            }
            case SELECT_POINT_B ->
                setUiState(UIState.NAVIGATE, view);
            case SHOW_PATH -> {
                setUiState(UIState.NAVIGATE, view);
            }
        }

        view.repaint();
    }

    // Behavior for clicking on attraction.
    // Also excuted by MapView's list of buttons.
    // Can execute pathfinding if the state is SELECT_POINT_B.
    public void handleAttractionClick(MapView view, int id) {
        if(uiState == UIState.SELECT_POINT_B && highlightedAttractionID != id) {
            currentRoute = preferTrains
                ? findTrainPath(highlightedAttractionID, id)
                : findPath(highlightedAttractionID, id, new int[]{});

            setUiState(UIState.SHOW_PATH, view);
        }
        else {
            highlightAttraction(id, view);
        }

        view.repaint();
    }

    // Event that get calls by MapView
    // Handles dragging of the map and collision detection for attraction nodes.
    public void handleMousePress(MouseEvent e, MapView view) {
        if(SwingUtilities.isRightMouseButton(e)
            && e.getX() >= MapView.MAP_X
            && e.getY() < MapView.MAP_HEIGHT
        ) {
            dragXStart = pixelToWorldX(e.getX());
            dragYStart = pixelToWorldY(e.getY());

            dragging = true;
        }
        else if(SwingUtilities.isLeftMouseButton(e)) {
            if(uiState != UIState.SELECT_POINT_B) {
                unhighlightAttraction(view);
            }

            double radius = MapView.ATTRACTION_RADIUS * mapScale;

            forEachAttraction(view, (x, y, id, name, highlighted) -> {
                double dx = mouseX - x;
                double dy = mouseY - y;

                if(dx * dx + dy * dy < radius * radius) {
                    handleAttractionClick(view, id);
                }
            });
        }

        view.repaint();
    }

    // Event called in MapView
    public void handleMouseRelease() {
        dragging = false;
    }

    // Event called in MapView
    public void handleMouseDrag(MapView view) {
        if(dragging) {
            double bounds = -1.0 / (mapScale * 2.0);

            cameraX = Math.clamp(dragXStart + cameraX - pixelToWorldX(mouseX), bounds, (1.0 + bounds));
            cameraY = Math.clamp(dragYStart + cameraY - pixelToWorldY(mouseY), bounds, (1.0 + bounds));
        }

        view.repaint();
    }

    // Event called in MapView
    public void handleMouseMove(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    // Event called in MapView
    // The map scale cannnot exceed upper and lower limits present below.
    public void handleMouseWheelMove(MouseWheelEvent e, MapView view) {
        if(e.getWheelRotation() > 0) {
            if(mapScale >= 2) {
                cameraX -= .5 / mapScale;
                cameraY -= .5 / mapScale;
                mapScale *= .5;
            }
        }
        else {
            if(mapScale <= 4) {
                mapScale *= 2;
                cameraX += .5 / mapScale;
                cameraY += .5 / mapScale;
            }
        }

        double bounds = -1.0 / (mapScale * 2.0);

        cameraX = Math.clamp(cameraX, bounds, (1.0 + bounds));
        cameraY = Math.clamp(cameraY, bounds, (1.0 + bounds));

        view.repaint();
    }

    // Modified version of Dijkstra's pathfinding algorithm.
    // Ranks paths based off of ETA and calculates ETA based off of path type.
    // Blocks paths depending on time-of-day and if it's a parade route.
    // Implementation of Dijkstra's algorithm was derived off of pseudocode from: https://en.wikipedia.org/wiki/Dijkstra's_algorithm
    public Route findPath(int startID, int endID, int[] excludedPathTypes) {
        Route result = new Route();

        LocalDate currentDate = LocalDate.now();
        boolean isWeekend = currentDate.getDayOfWeek().getValue() >= DayOfWeek.FRIDAY.getValue();

        LocalTime startTime = isWeekend ? PARADE_WEEKEND_START : PARADE_WEEKDAY_START;
        LocalTime endTime = isWeekend ? PARADE_WEEKEND_END : PARADE_WEEKDAY_END;
        LocalTime currentTime = LocalTime.now();

        int trainWaitTime = 0;
        try {
            trainWaitTime = getTrainWaitTime() * 60;
        } catch(IOException | InterruptedException e) {
            e.printStackTrace();
        }

        int sourceIndex = map.getAttractionIndexFromID(startID);
        int endIndex = map.getAttractionIndexFromID(endID);

        double[] eta = new double[map.getNodes().size()];
        double[] dist = new double[map.getNodes().size()];
        int[] prev = new int[map.getNodes().size()];

        class NodeETA {
            int nodeIndex;
            double eta;

            NodeETA(int nodeIndex, double eta) {
                this.nodeIndex = nodeIndex;
                this.eta = eta;
            }
        }

        PriorityQueue<NodeETA> Q = new PriorityQueue<>((a, b) -> Double.compare(a.eta, b.eta));

        for(int i = 0; i < map.getNodes().size(); i++) {
            eta[i] = Double.MAX_VALUE;
            dist[i] = 0;
            prev[i] = MapAttraction.ATTRACTION_ID_INVALID;
        }
        eta[sourceIndex] = 0;
        // I believe I can get away with this?
        Q.add(new NodeETA(sourceIndex, eta[sourceIndex]));

        while (!Q.isEmpty()) {
            NodeETA curr = Q.poll();
            int u = curr.nodeIndex;

            if (curr.eta > eta[u]) { continue; }
            if (u == endIndex)     { break; }

            MapNode node = map.getNodes().get(u);

            for (int i = 0; i < node.getConnections().size(); i++) {
                int type = node.getConnectionTypes().get(i);

                boolean typeExcluded = false;
                for (int t : excludedPathTypes) {
                    if(t == type) { typeExcluded = true; break; }
                }
                if(typeExcluded) { continue; }

                int v = node.getConnections().get(i);
                double dMeters = node.getDistances().get(i);

                double dSteps = dMeters * STEPS_PER_METER;
                double t;

                if (type == MapNode.CONNECTION_TYPE_TRAIN) {
                    t = trainWaitTime + (dMeters / 3) * STEPS_PER_METER / STEPS_PER_SECOND;
                    dSteps = 0;
                }
                else if (type == MapNode.CONNECTION_TYPE_PARADE
                    && currentTime.isAfter(startTime)
                    && currentTime.isBefore(endTime))
                {
                    continue;
                }
                else {
                    t = dMeters * STEPS_PER_METER / STEPS_PER_SECOND;
                }

                double alt = eta[u] + t;
                if (alt < eta[v]) {
                    eta[v] = alt;
                    dist[v] = dist[u] + dSteps;
                    prev[v] = u;
                    Q.add(new NodeETA(v, alt));
                }
            }
        }

        if(eta[endIndex] == Double.MAX_VALUE) {
            return result;
        }

        for (int i = endIndex; i != MapAttraction.ATTRACTION_ID_INVALID; i = prev[i]) {
            MapNode node = map.getNodes().get(i);
            result.path.add(new Point.Double(node.getX(), node.getY()));
        }
        Collections.reverse(result.path);
        result.distance = (int)(dist[endIndex]);
        result.eta = (int)(eta[endIndex]);

        return result;
    }

    // Used in prefer trains mode. Finds three paths using findPath, then stitches them together.
    public Route findTrainPath(int startAttractionID, int endAttractionID) {
        MapAttraction startAttraction = map.getAttractionFromID(startAttractionID);
        MapAttraction endAttraction = map.getAttractionFromID(endAttractionID);

        int startTrainID = startAttraction.isTrainStation() ? startAttractionID : startAttraction.getClosestTrainID();
        int endTrainID = endAttraction.isTrainStation() ? endAttractionID : endAttraction.getClosestTrainID();

        Route combined = new Route();

        Route walkToTrain = new Route();
        if(!startAttraction.isTrainStation()) {
            walkToTrain = findPath(startAttractionID, startTrainID, new int[]{MapNode.CONNECTION_TYPE_TRAIN});
            if(!walkToTrain.exists()) { return combined; }
        }

        Route trainToTrain = findPath(startTrainID, endTrainID, new int[]{MapNode.CONNECTION_TYPE_WALKWAY, MapNode.CONNECTION_TYPE_PARADE});
        if(!trainToTrain.exists()) { return combined; }

        Route walkFromTrain = new Route();
        if(!endAttraction.isTrainStation()) {
            walkFromTrain = endAttraction.isTrainStation() ? new Route() : findPath(endTrainID, endAttractionID, new int[]{MapNode.CONNECTION_TYPE_TRAIN});
            if(!walkFromTrain.exists()) { return combined; }
        }

        combined.eta = walkToTrain.eta + trainToTrain.eta + walkFromTrain.eta;
        combined.distance = walkToTrain.distance + trainToTrain.distance + walkFromTrain.distance;

        combined.path.addAll(walkToTrain.path);
        combined.path.addAll(trainToTrain.path);
        combined.path.addAll(walkFromTrain.path);

        return combined;
    }

    public void setPreferTrains(boolean preferTrains) {
        this.preferTrains = preferTrains;
    }

    public interface PathEvent {
        void execute(int x0, int y0, int x1, int y1);
    }

    // Similarly to forEachAttraction, converts line edge coordinates to pixel space.
    public void forEachPathPoint(PathEvent e) {
        if(uiState == UIState.SHOW_PATH) {
            for(int i = 0; i < currentRoute.path.size() - 1; i++) {
                int x0 = worldToPixelX(currentRoute.path.get(i).x);
                int y0 = worldToPixelY(currentRoute.path.get(i).y);

                int x1 = worldToPixelX(currentRoute.path.get(i + 1).x);
                int y1 = worldToPixelY(currentRoute.path.get(i + 1).y);

                e.execute(x0, y0, x1, y1);
            }
        }
    }

    // Calls adaptor's getWaitTime() and expects a result in seconds.
    public int getAttractionWaitTime(MapAttraction attraction) throws IOException, InterruptedException {
        return attraction.isTrainStation() ? getTrainWaitTime()
            : adaptor.getWaitTime(attraction.getLandID(), attraction.getAttractionID());
    }

    public int getTrainWaitTime() throws IOException, InterruptedException {
        return adaptor.getWaitTime(TRAIN_STATION_LAND_ID, TRAIN_STATION_ATTRACTION_ID);
    }

    public BufferedImage getMapImage() {
        return map.getMapImage();
    }

    public double getMapScale() {
        return mapScale;
    }
}
