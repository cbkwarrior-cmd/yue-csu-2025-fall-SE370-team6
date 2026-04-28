import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Stack;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class MapEditor extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
    final Dimension DISPLAY_DIMENSIONS = Toolkit.getDefaultToolkit().getScreenSize();
    final int WINDOW_HEIGHT = (int) DISPLAY_DIMENSIONS.getHeight() - 50;
    final int WINDOW_WIDTH = WINDOW_HEIGHT;

    final String NODES_FILE_NAME = "nodes.txt";

    final double PARK_WIDTH_IN_METERS = 1200;
    final double PARK_HEIGHT_IN_METERS = 800;

    static double cameraX = 0, cameraY = 0;
    static double scale = 1.0;

    static double dragXStart, dragYStart;
    static boolean dragging = false;

    static int mouseX, mouseY;
    static boolean lmbDown;

    static final int CONNECTION_TYPE_WALKWAY = 0;
    static final int CONNECTION_TYPE_TRAIN = 1;
    static final int CONNECTION_TYPE_PARADE = 2;

    static int connectionType = CONNECTION_TYPE_WALKWAY;

    private class Node {
        public int attractionID;
        public int landID;
        public double x, y;
        public String name;
        public ArrayList<Integer> connections;
        public ArrayList<Double> distances;
        public ArrayList<Integer> connectionTypes;
    }
    static ArrayList<Node> nodes = new ArrayList<>();

    static BufferedImage mapImage;

    enum EditorMode {
        MODE_ATTRACTION_PLACE(0),
        MODE_PATH_DRAW(1);

        public final int code;

        EditorMode(int code) {
            this.code = code;
        }

        public static EditorMode fromCode(int code) {
            for(EditorMode m : values()) {
                if(m.code == code) { return m; }
            }
            return MODE_ATTRACTION_PLACE;
        }
    };
    static EditorMode editorMode = EditorMode.MODE_ATTRACTION_PLACE;

    static Stack<Integer> undoStack = new Stack<>();

    static JLabel modeLabel;
    static JPanel dataPanel;
    static JTextField dataPanelNameField;
    static JTextField dataPanelAttractionField;
    static JTextField dataPanelLandField;

    static int pointAIndex = -1;

    private int worldToPixelX(double x) {
        return (int)((x + .5 - cameraX) * WINDOW_WIDTH * scale);
    }

    private int worldToPixelY(double y) {
        return (int)((y + .5 - cameraY) * WINDOW_HEIGHT * scale);
    }

    private double pixelToWorldX(int x) {
        return cameraX + ((double)x / WINDOW_WIDTH - .5) / scale;
    }

    private double pixelToWorldY(int y) {
        return cameraY + ((double)y / WINDOW_HEIGHT - .5) / scale;
    }

    private void placeNode() {
        Node n = new Node();
        n.attractionID = -1;
        n.landID = -1;
        n.x = pixelToWorldX(mouseX);
        n.y = pixelToWorldY(mouseY);
        n.name = "";
        n.connections = new ArrayList<>();
        n.distances = new ArrayList<>();
        n.connectionTypes = new ArrayList<>();
        nodes.add(n);
    }

    private void tracePath(int pointB) {
        double dx = nodes.get(pointB).x - nodes.get(pointAIndex).x;
        double dy = nodes.get(pointB).y - nodes.get(pointAIndex).y;
        double dist = Math.hypot(dx * PARK_WIDTH_IN_METERS, dy * PARK_HEIGHT_IN_METERS);

        nodes.get(pointB).connections.add(pointAIndex);
        nodes.get(pointAIndex).connections.add(pointB);

        nodes.get(pointB).distances.add(dist);
        nodes.get(pointAIndex).distances.add(dist);

        nodes.get(pointB).connectionTypes.add(connectionType);
        nodes.get(pointAIndex).connectionTypes.add(connectionType);

        pointAIndex = pointB;
    }

    public void mousePressed(MouseEvent e) {
        if(SwingUtilities.isRightMouseButton(e)) {
            dragXStart = pixelToWorldX(e.getX());
            dragYStart = pixelToWorldY(e.getY());
            dragging = true;
        }
        else if(SwingUtilities.isLeftMouseButton(e) && !dataPanel.isEnabled()) {
            if(editorMode == EditorMode.MODE_ATTRACTION_PLACE) {
                placeNode();
                undoStack.push(editorMode.code);
                setFieldPanelEnabled(true);
            }
            lmbDown = true;
            repaint();
        }
    }

    public void mouseReleased(MouseEvent e) {
        dragging = false;
        lmbDown = false;
    }

    public void mouseDragged(MouseEvent e) {
        if(dragging) {
            double bounds = -1.0 / (scale * 2.0);
            cameraX = Math.clamp(dragXStart + cameraX - pixelToWorldX(e.getX()), bounds, (1.0 + bounds));
            cameraY = Math.clamp(dragYStart + cameraY - pixelToWorldY(e.getY()), bounds, (1.0 + bounds));
            repaint();
        }
    }

    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        if(e.getWheelRotation() > 0) {
            cameraX -= .5 / scale;
            cameraY -= .5 / scale;
            scale *= .5;
        }
        else {
            scale *= 2;
            cameraX += .5 / scale;
            cameraY += .5 / scale;
        }

        repaint();
    }

    public void keyPressed(KeyEvent e) {
        if(dataPanel.isEnabled() || pointAIndex != -1) { return; }

        switch(e.getKeyCode()) {
            case KeyEvent.VK_1: {
                editorMode = EditorMode.MODE_ATTRACTION_PLACE;
            } break;
            case KeyEvent.VK_2: {
                editorMode = EditorMode.MODE_PATH_DRAW;
                connectionType = CONNECTION_TYPE_WALKWAY;
            } break;
            case KeyEvent.VK_3: {
                editorMode = EditorMode.MODE_PATH_DRAW;
                connectionType = CONNECTION_TYPE_TRAIN;
            } break;
            case KeyEvent.VK_4: {
                editorMode = EditorMode.MODE_PATH_DRAW;
                connectionType = CONNECTION_TYPE_PARADE;
            } break;
            case KeyEvent.VK_Z: {
                if(undoStack.isEmpty()) { break; }
                switch(EditorMode.fromCode(undoStack.pop())) {
                    case EditorMode.MODE_ATTRACTION_PLACE: {
                        nodes.removeLast();
                    } break;
                    case EditorMode.MODE_PATH_DRAW: {
                        int b = undoStack.pop();
                        int a = undoStack.pop();
                        nodes.get(a).connections.removeLast();
                        if(nodes.get(b).attractionID == -1) {
                            nodes.removeLast();
                        }
                        else {
                            nodes.get(b).connections.removeLast();
                        }
                    } break;
                }
            } break;
            case KeyEvent.VK_S: {
                try {
                    FileWriter wr = new FileWriter(NODES_FILE_NAME, false);
                    for (Node node : nodes) {
                        wr.write("n:\n");
                        wr.write(node.name + "\n");
                        wr.write("" + node.attractionID + "\n");
                        wr.write("" + node.landID + "\n");
                        wr.write("" + node.x + "\n");
                        wr.write("" + node.y + "\n");
                        for (Integer c : node.connections) {
                            wr.write("" + c + "\n");
                        }
                        wr.write("|\n");
                        for (Double d : node.distances) {
                            wr.write("" + d + "\n");
                        }
                        wr.write("|\n");
                        for (Integer t : node.connectionTypes) {
                            wr.write("" + t + "\n");
                        }
                        wr.write("|\n");
                    }
                    wr.close();
                }
                catch(IOException ex) {
                    ex.printStackTrace();
                }
                System.out.println("Wrote to " + NODES_FILE_NAME);
            } break;
            default: break;
        }
        updateModeLabel();
        repaint();
    }

    public void mouseClicked(MouseEvent e) { }
    public void mouseExited(MouseEvent e) { }
    public void mouseEntered(MouseEvent e) { }
    public void keyTyped(KeyEvent e) { }
    public void keyReleased(KeyEvent e) { }

    private void updateModeLabel() {
        switch(editorMode) {
            case MODE_ATTRACTION_PLACE: {
                modeLabel.setText("MODE: Attraction Place");
                modeLabel.setForeground(Color.GREEN);
            } break;
            case MODE_PATH_DRAW: {
                switch(connectionType) {
                    case CONNECTION_TYPE_WALKWAY: {
                        modeLabel.setText("MODE: Walkway Draw");
                        modeLabel.setForeground(Color.BLUE);
                    } break;
                    case CONNECTION_TYPE_TRAIN: {
                        modeLabel.setText("MODE: Train Draw");
                        modeLabel.setForeground(Color.RED);
                    } break;
                    case CONNECTION_TYPE_PARADE: {
                        modeLabel.setText("MODE: Parade Draw");
                        modeLabel.setForeground(Color.CYAN);
                    } break;
                }
            } break;
        }
    }

    private void setFieldPanelEnabled(boolean enabled) {
        dataPanelNameField.setText("");
        dataPanelAttractionField.setText("");
        dataPanelLandField.setText("");
        dataPanel.setEnabled(enabled);
        dataPanel.setVisible(enabled);
        for (Component c : dataPanel.getComponents()) {
            c.setEnabled(enabled);
            c.setVisible(enabled);
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D)g;

        g2.drawImage(mapImage, worldToPixelX(-.5), worldToPixelY(-.5), (int)(WINDOW_WIDTH * scale), (int)(WINDOW_HEIGHT * scale), null);

        boolean nodeClicked = false;

        for(int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);

            int diameter = (int)(20 * scale), radius = diameter / 2;
            int x = worldToPixelX(node.x);
            int y = worldToPixelY(node.y);

            for(int j = 0; j < node.connections.size(); j++) {
                switch(node.connectionTypes.get(j)) {
                    case CONNECTION_TYPE_WALKWAY: g.setColor(Color.BLUE); break;
                    case CONNECTION_TYPE_TRAIN:   g.setColor(Color.RED);  break;
                    case CONNECTION_TYPE_PARADE:  g.setColor(Color.CYAN); break;
                }

                int destIndex = node.connections.get(j);

                float width = 3f;
                BasicStroke stroke = new BasicStroke(width);
                g2.setStroke(stroke);

                Node dest = nodes.get(destIndex);
                g2.drawLine(x, y, worldToPixelX(dest.x), worldToPixelY(dest.y));
            }

            if(node.attractionID == -1) {
                if(editorMode != EditorMode.MODE_PATH_DRAW) { continue; }
                diameter *= .5;
                radius *= .5;
            }

            if(lmbDown && editorMode == EditorMode.MODE_PATH_DRAW) {
                double dx = mouseX - x, dy = mouseY - y;
                if(dx * dx + dy * dy < radius * radius) {
                    if(pointAIndex == -1) {
                        pointAIndex = i;
                    }
                    else if(pointAIndex != i){
                        undoStack.push(pointAIndex);
                        undoStack.push(i);
                        undoStack.push(editorMode.code);
                        tracePath(i);
                        pointAIndex = -1;
                        lmbDown = false;
                        repaint();
                        return;
                    }
                    nodeClicked = true;
                }
            }

            int drawX = x - radius, drawY = y - radius;
            g2.setColor(Color.BLACK);
            g2.fillOval(drawX, drawY, diameter, diameter);
            g2.setColor(Color.WHITE);
            g2.fillOval(drawX + 3, drawY + 3, diameter - 6, diameter - 6);
        }

        if(lmbDown && pointAIndex != -1 && !nodeClicked) {
            placeNode();
            undoStack.push(pointAIndex);
            undoStack.push(nodes.size() - 1);
            undoStack.push(editorMode.code);
            tracePath(nodes.size() - 1);
            lmbDown = false;
            repaint();
        }
    }

    public MapEditor() {
        try {
            mapImage = ImageIO.read(new File("map.jpeg"));
        } catch(IOException e) {
            e.printStackTrace();
        }

        try {
            BufferedReader r = new BufferedReader(new FileReader(NODES_FILE_NAME));

            String ln;
            while((ln = r.readLine()) != null) {
                Node n = new Node();
                n.name = r.readLine();
                n.attractionID = Integer.parseInt(r.readLine());
                n.landID = Integer.parseInt(r.readLine());
                n.x = Double.parseDouble(r.readLine());
                n.y = Double.parseDouble(r.readLine());
                n.connections = new ArrayList<>();
                while(!(ln = r.readLine()).contains("|")) {
                    n.connections.add(Integer.parseInt(ln));
                }
                n.distances = new ArrayList<>();
                while(!(ln = r.readLine()).contains("|")) {
                    n.distances.add(Double.parseDouble(ln));
                }
                n.connectionTypes = new ArrayList<>();
                while(!(ln = r.readLine()).contains("|")) {
                    n.connectionTypes.add(Integer.parseInt(ln));
                }
                nodes.add(n);
            }

            r.close();
        }
        catch(IOException ex) {
            System.out.println("Missing nodes.txt, assuming empty");
        }

        this.setFocusable(true);
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.addMouseWheelListener(this);
        this.addKeyListener(this);
        this.setBackground(Color.BLACK);
        this.setLayout(null);

        modeLabel = new JLabel();
        modeLabel.setFont(new Font("Arial", Font.BOLD, 30));
        modeLabel.setBounds(0, 0, WINDOW_WIDTH, 30);
        modeLabel.setBackground(Color.WHITE);
        updateModeLabel();
        this.add(modeLabel);

        dataPanel = new JPanel(new GridBagLayout());
        dataPanel.setBounds(0, WINDOW_HEIGHT - 60, WINDOW_WIDTH, 30);
        dataPanel.setBackground(Color.WHITE);

        // This all sucked to implement
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 5, 0, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 0;

        gbc.gridx = 0;
        gbc.weightx = 0;
        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setFont(new Font("Arial", Font.BOLD, 15));
        dataPanel.add(nameLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        dataPanelNameField = new JTextField();
        dataPanelNameField.setFont(new Font("Arial", Font.BOLD, 15));
        dataPanel.add(dataPanelNameField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        JLabel attractionLabel = new JLabel("Attr ID:");
        attractionLabel.setFont(new Font("Arial", Font.BOLD, 15));
        dataPanel.add(attractionLabel, gbc);

        gbc.gridx = 3;
        gbc.weightx = 1;
        dataPanelAttractionField = new JTextField();
        dataPanelAttractionField.setFont(new Font("Arial", Font.BOLD, 15));
        dataPanel.add(dataPanelAttractionField, gbc);

        gbc.gridx = 4;
        gbc.weightx = 0;
        JLabel landLabel = new JLabel("Land ID:");
        landLabel.setFont(new Font("Arial", Font.BOLD, 15));
        dataPanel.add(landLabel, gbc);

        gbc.gridx = 5;
        gbc.weightx = 1;
        dataPanelLandField = new JTextField();
        dataPanelLandField.setFont(new Font("Arial", Font.BOLD, 15));
        dataPanel.add(dataPanelLandField, gbc);

        gbc.gridx = 6;
        gbc.weightx = 0;
        JButton fieldPanelButton = new JButton("Enter");
        fieldPanelButton.setFont(new Font("Arial", Font.BOLD, 15));
        fieldPanelButton.addActionListener(e -> {
            nodes.getLast().name = dataPanelNameField.getText();
            try {
                nodes.getLast().attractionID = Integer.parseInt(dataPanelAttractionField.getText());
                nodes.getLast().landID = Integer.parseInt(dataPanelLandField.getText());
            } catch(Exception ex) {
                return;
            }
            setFieldPanelEnabled(false);
            repaint();
        });
        dataPanel.add(fieldPanelButton, gbc);

        this.add(dataPanel);
        setFieldPanelEnabled(false);

        JFrame frame = new JFrame("Editor");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.add(this);
        frame.setVisible(true);
        frame.setResizable(false);
    }

    public static void main(String[] args) {
        new MapEditor();
    }
}
