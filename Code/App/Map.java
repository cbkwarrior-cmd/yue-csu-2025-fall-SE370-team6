import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class Map {
    private ArrayList<MapNode> nodes;
    private BufferedImage mapImage;

    public Map(ArrayList<MapNode> nodes, BufferedImage mapImage) {
        this.nodes = nodes;
        this.mapImage = mapImage;
    }

    public ArrayList<MapNode> getNodes() {
        return nodes;
    }

    public BufferedImage getMapImage() {
        return mapImage;
    }
}
