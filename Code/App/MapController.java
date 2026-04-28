import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.function.BiConsumer;

import javax.imageio.ImageIO;

public class MapController {
    private Map map;

    // Key-value pairs of [attractionID : index into map's list of nodes]
    private HashMap<Integer, Integer> attractionsTable = new HashMap<>();

    private int pathDistance;
    private int pathETA;

    public MapController() {
    }

    public void loadMapFromFiles(String mapImagePath, String mapNodesPath) {
        try {
            BufferedImage mapImage = ImageIO.read(new File(mapImagePath));

            ArrayList<MapNode> nodes = new ArrayList<>();
            BufferedReader nodesReader = new BufferedReader(new FileReader(mapNodesPath));

            String ln;
            while((ln = nodesReader.readLine()) != null) {
                String name = nodesReader.readLine();
                int attractionID = Integer.parseInt(nodesReader.readLine());
                int landID = Integer.parseInt(nodesReader.readLine());
                double x = Double.parseDouble(nodesReader.readLine());
                double y = Double.parseDouble(nodesReader.readLine());

                MapNode node = new MapNode(name, attractionID, landID, x, y);

                while(!(ln = nodesReader.readLine()).contains("|")) {
                    node.getConnections().add(Integer.parseInt(ln));
                }
                while(!(ln = nodesReader.readLine()).contains("|")) {
                    node.getDistances().add(Double.parseDouble(ln));
                }

                nodes.add(node);
                if(attractionID != -1) {
                    attractionsTable.put(attractionID, nodes.size() - 1);
                    System.out.println("" + attractionID + " " + (nodes.size() - 1));
                }
            }

            nodesReader.close();

            map = new Map(nodes, mapImage);
            System.out.println(map.getNodes().size());
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<Point.Double> findPath(int startID, int endID) {
        // Implementation of Dijkstra's algo sourced from: https://en.wikipedia.org/wiki/Dijkstra%27s_algorithm

        int sourceIndex = attractionsTable.get(startID);
        int endIndex = attractionsTable.get(endID);
        ArrayList<Point.Double> path = new ArrayList<>();

        double[] dist = new double[map.getNodes().size()];
        int[] eta = new int[map.getNodes().size()];
        int[] prev = new int[map.getNodes().size()];
        PriorityQueue<Integer> Q = new PriorityQueue<>((a, b) -> Double.compare(dist[a], dist[b]));

        for(int i = 0; i < map.getNodes().size(); i++) {
            dist[i] = Double.MAX_VALUE;
            eta[i] = 0;
            prev[i] = -1;
        }
        dist[sourceIndex] = 0;
        // I believe I can get away with this?
        Q.add(sourceIndex);

        while (!Q.isEmpty()) {
            int u = Q.poll();
            if (dist[u] == Double.MAX_VALUE) { continue; }
            if (u == endIndex) { break; }

            MapNode node = map.getNodes().get(u);

            for (int i = 0; i < node.getConnections().size(); i++) {
                int v = node.getConnections().get(i);

                double d = node.getDistances().get(i);
                double alt = dist[u] + d;
                if (alt < dist[v]) {
                    dist[v] = alt;
                    eta[v] = eta[u] + 1;
                    prev[v] = u;
                    Q.add(v);
                }
            }
        }

        for (int i = endIndex; i != -1; i = prev[i]) {
            MapNode node = map.getNodes().get(i);
            path.add(new Point.Double(node.getX(), node.getY()));
        }

        pathDistance = (int)(dist[endIndex] * 1000);
        pathETA = eta[endIndex];

        Collections.reverse(path);
        return path;
    }

    public void forEachAttraction(BiConsumer<Integer, MapNode> action) {
        for(java.util.Map.Entry<Integer, Integer> e : attractionsTable.entrySet()) {
            action.accept(e.getKey(), map.getNodes().get(e.getValue()));
        }
    }

    public MapNode getAttractionFromID(int id) {
        return map.getNodes().get(attractionsTable.get(id));
    }

    // Note for Morgan: Here are the methods to put your code into.
    // If this isn't structured how you'd want it to be, change it.
    public int getAttractionWaitTime(MapNode node) {
        return 0;
    }

    public void fetchFromHTTP() {
    }

    public BufferedImage getMapImage() {
        return map.getMapImage();
    }

    public double getPathDistance() {
        return pathDistance;
    }

    public int getPathETA() {
        return pathETA;
    }
}
