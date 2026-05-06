package org.DisneylandMap;

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
    private final String MAP_IMAGE_PATH = "map.jpeg";
    private final String MAP_NODES_PATH = "nodes.txt";

    private final double STEPS_PER_METER = 1.31;
    private final double STEPS_PER_SECOND = 1.5;

    private final LocalTime PARADE_WEEKDAY_START = LocalTime.of(20, 30);
    private final LocalTime PARADE_WEEKDAY_END = LocalTime.of(21, 10);

    private final LocalTime PARADE_WEEKEND_START = LocalTime.of(22, 30);
    private final LocalTime PARADE_WEEKEND_END = LocalTime.of(23, 10);

    private final int NUM_TRAIN_STATIONS = 4;
    private final int TRAIN_STATION_LAND_ID = 113;
    private final int TRAIN_STATION_ATTRACTION_ID = 674;

    private IQueueTime adaptor;
    private Map map;

    private int routeETA;
    private int routeDist;
    private ArrayList<Point.Double> routePath = new ArrayList<>();
    private boolean preferTrains;

    private double cameraX = 0, cameraY = 0;
    private double scale = 1.0;

    private int mouseX, mouseY;
    private double dragXStart, dragYStart;
    private boolean dragging = false;

    private enum UIState {
        NAVIGATE,
        SELECT_POINT_B,
        SHOW_PATH,
    };
    private UIState uiState = UIState.NAVIGATE;
    private int highlightedAttractionID;

    public MapController(MapView view) {
        this.adaptor = new QueueTimesAdaptor();
        this.map = new Map(MAP_IMAGE_PATH, MAP_NODES_PATH);
        unhighlightAttraction(view);
    }

    public int worldToPixelX(double x) {
        return MapView.MAP_X + (int)((x + .5 - cameraX) * MapView.MAP_WIDTH * scale);
    }

    public int worldToPixelY(double y) {
        return MapView.MAP_Y + (int)((y + .5 - cameraY) * MapView.MAP_HEIGHT * scale);
    }

    private double pixelToWorldX(int x) {
        return cameraX + ((double)(x - MapView.MAP_X) / MapView.MAP_WIDTH - .5) / scale;
    }

    private double pixelToWorldY(int y) {
        return cameraY + ((double)(y - MapView.MAP_Y) / MapView.MAP_HEIGHT - .5) / scale;
    }

    private void highlightAttraction(int id, MapView view) {
        this.highlightedAttractionID = id;
        try {
            MapAttraction attraction = map.getAttractionFromID(highlightedAttractionID);
            String info = "" + attraction.getName() + ":\n";
            int waitTimeMinutes = getAttractionWaitTime(attraction);
            if(waitTimeMinutes == -2) {
                info += "Error: Failed to get wait time\n";
            }
            else if(waitTimeMinutes == -1) {
                info += "This attraction is closed.";
            }
            else {
                info += "Wait Time: " + waitTimeMinutes + " Minutes\n";
            }
            view.setAttractionInfo(info);
        } catch(IOException | InterruptedException e) {
            e.printStackTrace();
        }
        setUiState(uiState, view);
    }

    public void unhighlightAttraction(MapView view) {
        this.highlightedAttractionID = -1;
        view.setAttractionInfo("Press Node to View Attraction Info");
        setUiState(uiState, view);
    }

    public interface AttractionEvent {
        void execute(int pixelX, int pixelY, int id, String name, boolean highlighted);
    }

    public void forEachAttraction(MapView view, AttractionEvent e) {
        map.forEachAttraction((id, attraction) -> {
            int x = worldToPixelX(attraction.getX());
            int y = worldToPixelY(attraction.getY());

            if(x < MapView.MAP_X - MapView.ATTRACTION_DIAMETER || x >= MapView.MAP_X + MapView.MAP_WIDTH + MapView.ATTRACTION_DIAMETER || y < MapView.MAP_Y - MapView.ATTRACTION_DIAMETER || y >= MapView.MAP_Y + MapView.MAP_HEIGHT + MapView.ATTRACTION_DIAMETER) {
                return;
            }

            e.execute(x, y, id, attraction.getName(), id == highlightedAttractionID);
        });
    }

    private void setUiState(UIState newState, MapView view) {
        this.uiState = newState;

        switch(uiState) {
            case NAVIGATE: {
                if(highlightedAttractionID == -1) {
                    view.updateRouteButton(MapView.GREY_RGB, Color.BLACK, "Requires Start Point to Route");
                }
                else {
                    view.updateRouteButton(MapView.GREEN_RGB, Color.WHITE, "Begin Routing");
                }
            } break;
            case SELECT_POINT_B: {
                view.updateRouteButton(Color.BLACK, Color.WHITE, "Cancel");
            } break;
            case SHOW_PATH: {
                view.updateRouteButton(MapView.BLUE_RGB, Color.WHITE, "Clear Path");
            } break;
        }

        if(this.uiState == UIState.SHOW_PATH) {
            String etaText = "Impossible route";
            String distanceText = "Impossible route";

            if(!routePath.isEmpty()) {
                int etaMinutes = routeETA / 60;

                if(etaMinutes == 0) {
                    etaText = "" + routeETA + " sec" + (routeETA == 1 ? "" : "s");
                }
                else {
                    etaText = "" + etaMinutes + " min" + (etaMinutes == 1 ? "" : "s");

                    int etaHours = etaMinutes / 60;
                    if(etaHours != 0) {
                        etaText = "" + etaHours + " hr" + (etaHours == 1 ? "" : "s") + " " + etaText;
                    }
                }

                distanceText = "" + routeDist + " steps";
            }

            view.updateRouteLabels(etaText, distanceText);
        }
        else {
            view.updateRouteLabels("N/A", "N/A");
        }
    }

    public void handleRouteButton(MapView view) {
        switch (uiState) {
            case NAVIGATE: {
                if (highlightedAttractionID == -1) { break; }
                setUiState(UIState.SELECT_POINT_B, view);
            } break;
            case SELECT_POINT_B: {
                setUiState(UIState.NAVIGATE, view);
            } break;
            case SHOW_PATH: {
                setUiState(UIState.NAVIGATE, view);
            } break;
        }
        view.repaint();
    }

    public void handleAttractionClick(MapView view, int id) {
        if (uiState == UIState.SELECT_POINT_B && highlightedAttractionID != id) {
            if(preferTrains && highlightedAttractionID > -1 && id > -1) {
                findTrainPath(highlightedAttractionID, id);
            }
            else {
                findPath(highlightedAttractionID, id, new int[]{});
            }
            setUiState(UIState.SHOW_PATH, view);
        }
        else {
            highlightAttraction(id, view);
        }
        view.repaint();
    }

    public void handleMousePress(MouseEvent e, MapView view) {
        if(SwingUtilities.isRightMouseButton(e) && e.getX() >= MapView.MAP_X && e.getY() < MapView.MAP_HEIGHT) {
            dragXStart = pixelToWorldX(e.getX());
            dragYStart = pixelToWorldY(e.getY());
            dragging = true;
        }
        else if(SwingUtilities.isLeftMouseButton(e)) {
            if(uiState != UIState.SELECT_POINT_B) {
                unhighlightAttraction(view);
            }

            forEachAttraction(view, (x, y, id, name, highlighted) -> {
                double dx = mouseX - x, dy = mouseY - y;
                double r = view.getAttractionRadius() * scale;

                if(dx * dx + dy * dy < r * r) {
                    handleAttractionClick(view, id);
                }
            });
        }
        view.repaint();
    }

    public void handleMouseRelease() {
        dragging = false;
    }

    public void handleMouseDrag(MapView view) {
        if(dragging) {
            double bounds = -1.0 / (scale * 2.0);
            cameraX = Math.clamp(dragXStart + cameraX - pixelToWorldX(mouseX), bounds, (1.0 + bounds));
            cameraY = Math.clamp(dragYStart + cameraY - pixelToWorldY(mouseY), bounds, (1.0 + bounds));
        }
        view.repaint();
    }

    public void handleMouseMove(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    public void handleMouseWheelMove(MouseWheelEvent e, MapView view) {
        if(e.getWheelRotation() > 0) {
            if(scale >= 2) {
                cameraX -= .5 / scale;
                cameraY -= .5 / scale;
                scale *= .5;
            }
        }
        else if(scale <= 4) {
            scale *= 2;
            cameraX += .5 / scale;
            cameraY += .5 / scale;
        }

        double bounds = -1.0 / (scale * 2.0);
        this.cameraX = Math.clamp(cameraX, bounds, (1.0 + bounds));
        this.cameraY = Math.clamp(cameraY, bounds, (1.0 + bounds));
        view.repaint();
    }

    // Implementation of Dijkstra's algorithm sourced from: https://en.wikipedia.org/wiki/Dijkstra%27s_algorithm
    public void findPath(int startID, int endID, int[] excludedPathTypes) {
        routePath.clear();

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
            prev[i] = -1;
        }
        eta[sourceIndex] = 0;
        // I believe I can get away with this?
        Q.add(new NodeETA(sourceIndex, eta[sourceIndex]));

        while (!Q.isEmpty()) {
            NodeETA curr = Q.poll();
            int u = curr.nodeIndex;

            if (curr.eta > eta[u]) { continue; }
            if (u == endIndex) { break; }

            MapNode node = map.getNodes().get(u);

            for (int i = 0; i < node.getConnections().size(); i++) {
                int type = node.getConnectionTypes().get(i);
                boolean typeExcluded = false;
                for (int t : excludedPathTypes) {
                    if (t == type) {
                        typeExcluded = true;
                        break;
                    }
                }
                if (typeExcluded) { continue; }

                int v = node.getConnections().get(i);
                double w = node.getDistances().get(i);

                double d = w * STEPS_PER_METER;
                double t;

                if (type == MapAttraction.CONNECTION_TYPE_TRAIN) {
                    t = trainWaitTime + (w / 3) * STEPS_PER_METER / STEPS_PER_SECOND;
                    d = 0;
                }
                else if (type == MapAttraction.CONNECTION_TYPE_PARADE
                        && currentTime.isAfter(startTime) && currentTime.isBefore(endTime)) {
                    continue;
                }
                else {
                    t = w * STEPS_PER_METER / STEPS_PER_SECOND;
                }

                double alt = eta[u] + t;
                if (alt < eta[v]) {
                    eta[v] = alt;
                    dist[v] = dist[u] + d;
                    prev[v] = u;
                    Q.add(new NodeETA(v, alt));
                }
            }
        }

        if(eta[endIndex] == Double.MAX_VALUE) {
            routePath.clear();
        }

        for (int i = endIndex; i != -1; i = prev[i]) {
            MapNode node = map.getNodes().get(i);
            routePath.add(new Point.Double(node.getX(), node.getY()));
        }

        Collections.reverse(routePath);

        routeDist = (int)(dist[endIndex]);
        routeETA = (int)(eta[endIndex]);
    }

    public void findTrainPath(int startAttractionID, int endAttractionID) {
        int startTrain = map.getAttractionFromID(startAttractionID).getClosestTrainID();
        int endTrain = map.getAttractionFromID(endAttractionID).getClosestTrainID();

        System.out.println("" + startAttractionID + " " + endAttractionID + " " + startTrain + " " + endTrain);

        int combinedETA = 0, combinedDist = 0;
        ArrayList<Point.Double> combinedPath = new ArrayList<>();

        findPath(startAttractionID, startTrain, new int[]{MapAttraction.CONNECTION_TYPE_TRAIN});
        combinedETA += routeETA;
        combinedDist += routeDist;
        combinedPath.addAll(routePath);

        findPath(startTrain, endTrain, new int[]{MapAttraction.CONNECTION_TYPE_WALKWAY, MapAttraction.CONNECTION_TYPE_PARADE});
        combinedETA += routeETA;
        combinedDist += routeDist;
        combinedPath.addAll(routePath);

        findPath(endTrain, endAttractionID, new int[]{MapAttraction.CONNECTION_TYPE_TRAIN});
        combinedETA += routeETA;
        combinedDist += routeDist;
        combinedPath.addAll(routePath);

        routeETA = combinedETA;
        routeDist = combinedDist;
        routePath.clear();
        routePath.addAll(combinedPath);
    }

    public void setPreferTrains(boolean preferTrains) {
        this.preferTrains = preferTrains;
    }

    public interface PathEvent {
        public void execute(int x0, int y0, int x1, int y1);
    }

    public void forEachPathPoint(PathEvent e) {
        if(uiState == UIState.SHOW_PATH) {
            for(int i = 0; i < routePath.size() - 1; i++) {
                int x0 = worldToPixelX(routePath.get(i).x);
                int y0 = worldToPixelY(routePath.get(i).y);
                int x1 = worldToPixelX(routePath.get(i + 1).x);
                int y1 = worldToPixelY(routePath.get(i + 1).y);
                e.execute(x0, y0, x1, y1);
            }
        }
    }

    public int getAttractionWaitTime(MapAttraction attraction) throws IOException, InterruptedException {
        boolean isTrainStation = attraction.getAttractionID() < -1 && attraction.getAttractionID() >= -1 - NUM_TRAIN_STATIONS;

        return isTrainStation ? getTrainWaitTime() : adaptor.getWaitTime(attraction.getLandID(), attraction.getAttractionID());
    }

    public int getTrainWaitTime() throws IOException, InterruptedException {
        return adaptor.getWaitTime(TRAIN_STATION_LAND_ID, TRAIN_STATION_ATTRACTION_ID);
    }

    public BufferedImage getMapImage() {
        return map.getMapImage();
    }

    public double getMapScale() {
        return scale;
    }
}
