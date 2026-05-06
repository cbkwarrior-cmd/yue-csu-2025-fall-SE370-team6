package org.DisneylandMap;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.BiConsumer;
import javax.imageio.ImageIO;

public class Map {
    private ArrayList<MapNode> nodes = new ArrayList<>();
    private BufferedImage mapImage;

    private HashMap<Integer, Integer> attractionsTable = new HashMap<>();

    public Map(String mapImagePath, String mapNodesPath) {
        try {
            this.mapImage = ImageIO.read(new File(mapImagePath));

            BufferedReader nodesReader = new BufferedReader(new FileReader(mapNodesPath));

            String ln;
            while((ln = nodesReader.readLine()) != null) {
                String name = nodesReader.readLine();
                int attractionID = Integer.parseInt(nodesReader.readLine());
                int closestTrainID = Integer.parseInt(nodesReader.readLine());
                int landID = Integer.parseInt(nodesReader.readLine());
                double x = Double.parseDouble(nodesReader.readLine());
                double y = Double.parseDouble(nodesReader.readLine());

                MapNode node;

                if(attractionID == -1) {
                    node = new MapNode(x, y);
                }
                else {
                    node = new MapAttraction(name, attractionID, landID, closestTrainID, x, y);
                    attractionsTable.put(attractionID, nodes.size());
                }

                while(!(ln = nodesReader.readLine()).contains("|")) {
                    node.getConnections().add(Integer.parseInt(ln));
                }
                while(!(ln = nodesReader.readLine()).contains("|")) {
                    node.getDistances().add(Double.parseDouble(ln));
                }
                while(!(ln = nodesReader.readLine()).contains("|")) {
                    node.getConnectionTypes().add(Integer.parseInt(ln));
                }

                nodes.add(node);
            }

            nodesReader.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void forEachAttraction(BiConsumer<Integer, MapAttraction> action) {
        for(java.util.Map.Entry<Integer, Integer> e : attractionsTable.entrySet()) {
            action.accept(e.getKey(), (MapAttraction)nodes.get(e.getValue()));
        }
    }

    public ArrayList<MapNode> getNodes() {
        return nodes;
    }

    public int getAttractionIndexFromID(int id) {
        return attractionsTable.get(id);
    }

    public MapAttraction getAttractionFromID(int id) {
        return (MapAttraction)nodes.get(getAttractionIndexFromID(id));
    }

    public BufferedImage getMapImage() {
        return mapImage;
    }
}
