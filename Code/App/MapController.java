import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.BiConsumer;

import javax.imageio.ImageIO;

public class MapController {
    private Map map;

    // Key-value pairs of [attractionID : index into map's list of nodes]
    private HashMap<Integer, Integer> attractionsTable = new HashMap<>();

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
        return new ArrayList<>();
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
}
