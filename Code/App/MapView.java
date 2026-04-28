import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class MapView extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener {
    private static final Dimension DISPLAY_DIMENSIONS = Toolkit.getDefaultToolkit().getScreenSize();
    private static final int WINDOW_HEIGHT = (int) DISPLAY_DIMENSIONS.getHeight() - 50;
    private static final int WINDOW_WIDTH = WINDOW_HEIGHT;
    private static final int MAP_WIDTH = (int)(WINDOW_WIDTH / 1.5);
    private static final int MAP_HEIGHT = (int)(WINDOW_HEIGHT / 1.5);
    private static final int MAP_X = WINDOW_HEIGHT - MAP_WIDTH;
    private static final int MAP_Y = 0;

    private MapController controller = new MapController();

    private double cameraX = 0, cameraY = 0;
    private double scale = 1.0;

    private double dragXStart, dragYStart;
    private boolean dragging = false;

    private int mouseX, mouseY;
    private boolean lmbDown;

    private int highlightedAttractionID;
    private ArrayList<Point.Double> path = new ArrayList<>();

    private enum UIState {
        NAVIGATE,
        SELECT_POINT_B,
        SHOW_PATH,
    };
    private UIState uiState = UIState.NAVIGATE;

    private JPanel attractionsPanel = new JPanel();
    private ArrayList<JButton> attractionButtons = new ArrayList<>();
    private JLabel routeEtaLabel = new JLabel();
    private JLabel routeDistLabel = new JLabel();
    private JTextArea attractionInfoLabel = new JTextArea();
    private JButton routeButton = new JButton();

    public MapView(String mapImagePath, String mapNodesPath) {
        controller = new MapController();
        controller.loadMapFromFiles(mapImagePath, mapNodesPath);
        unhighlightAttraction();
    }

    private int world_to_pixel_x(double x) {
        return MAP_X + (int)((x + .5 - cameraX) * MAP_WIDTH * scale);
    }

    private int world_to_pixel_y(double y) {
        return MAP_Y + (int)((y + .5 - cameraY) * MAP_HEIGHT * scale);
    }

    private double pixel_to_world_x(int x) {
        return cameraX + ((double)(x - MAP_X) / MAP_WIDTH - .5) / scale;
    }

    private double pixel_to_world_y(int y) {
        return cameraY + ((double)(y - MAP_Y) / MAP_HEIGHT - .5) / scale;
    }

    private void highlightAttraction(int id) {
        this.highlightedAttractionID = id;
        //TODO: Set text to highlighted attraction description:
        this.attractionInfoLabel.setText("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.");
        this.attractionInfoLabel.setCaretPosition(0);
        setUiState(uiState);
    }

    private void unhighlightAttraction() {
        this.highlightedAttractionID = -1;
        attractionInfoLabel.setText("[No Attraction Highlighted]");
        setUiState(uiState);
    }

    private void setUiState(UIState newState) {
        this.uiState = newState;

        switch(uiState) {
            case NAVIGATE: {
                if(highlightedAttractionID == -1) {
                    this.routeButton.setBackground(new Color(178, 178, 178));
                    this.routeButton.setText("(Requires Start Point to Route)");
                }
                else {
                    this.routeButton.setBackground(Color.GREEN);
                    this.routeButton.setText("Begin Routing");
                }
            } break;
            case SELECT_POINT_B: {
                this.routeButton.setBackground(new Color(183, 51, 75));
                this.routeButton.setText("Cancel");
            } break;
            case SHOW_PATH: {
                this.routeButton.setBackground(new Color(51, 154, 183));
                this.routeButton.setText("Clear Path");
            } break;
        }

        for (JButton button : attractionButtons) {
            button.setBackground(this.uiState == UIState.SELECT_POINT_B ? Color.LIGHT_GRAY : Color.BLUE);
        }

        if(this.uiState == UIState.SHOW_PATH) {
            this.routeEtaLabel.setText("10000");
            this.routeDistLabel.setText("10000");
        }
        else {
            this.routeEtaLabel.setText("N/A");
            this.routeDistLabel.setText("N/A");
        }
    }

    private void handleRouteButton() {
        switch (uiState) {
            case NAVIGATE: {
                if (highlightedAttractionID == -1) { break; }
                setUiState(UIState.SELECT_POINT_B);
            } break;
            case SELECT_POINT_B: {
                setUiState(UIState.NAVIGATE);
            } break;
            case SHOW_PATH: {
                path.clear();
                setUiState(UIState.NAVIGATE);
                repaint();
            } break;
        }
    }

    private void handleNodeClick(int id) {
        if (uiState == UIState.SELECT_POINT_B && highlightedAttractionID != id) {
            path = controller.findPath(highlightedAttractionID, id);
            setUiState(UIState.SHOW_PATH);
        } else {
            highlightAttraction(id);
        }
    }

    public void mousePressed(MouseEvent e) {
        if(SwingUtilities.isRightMouseButton(e) && e.getX() >= MAP_X && e.getY() < MAP_HEIGHT) {
            this.dragXStart = pixel_to_world_x(e.getX());
            this.dragYStart = pixel_to_world_y(e.getY());
            this.dragging = true;
        }
        else if(SwingUtilities.isLeftMouseButton(e)) {
            this.lmbDown = true;
            if(uiState != UIState.SELECT_POINT_B) {
                unhighlightAttraction();
            }
            repaint();
        }
    }

    public void mouseReleased(MouseEvent e) {
        this.dragging = false;
        this.lmbDown = false;
    }

    public void mouseDragged(MouseEvent e) {
        if(dragging) {
            double bounds = -1.0 / (scale * 2.0);
            this.cameraX = Math.clamp(dragXStart + cameraX - pixel_to_world_x(e.getX()), bounds, (1.0 + bounds));
            this.cameraY = Math.clamp(dragYStart + cameraY - pixel_to_world_y(e.getY()), bounds, (1.0 + bounds));
            repaint();
        }
    }

    public void mouseMoved(MouseEvent e) {
        this.mouseX = e.getX();
        this.mouseY = e.getY();
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
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
        repaint();
    }

    public void mouseClicked(MouseEvent e) { }
    public void mouseExited(MouseEvent e) { }
    public void mouseEntered(MouseEvent e) { }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D)g;

        g2.drawImage(controller.getMapImage(), world_to_pixel_x(-.5), world_to_pixel_y(-.5), (int)(MAP_WIDTH * scale), (int)(MAP_HEIGHT * scale), null);

        drawNodes(g2);
        drawPath(g2);

        g2.setColor(Color.GRAY);
        g2.fillRect(0, 0, MAP_X, WINDOW_HEIGHT);

        g2.setColor(Color.GRAY);
        g2.fillRect(0, MAP_HEIGHT, WINDOW_WIDTH, WINDOW_HEIGHT - MAP_HEIGHT);
    }

    private void drawNodes(Graphics2D g) {
        controller.forEachAttraction((id, node) -> {
            final int DIAMETER = (int)(20 * scale), RADIUS = DIAMETER / 2;
            int x = world_to_pixel_x(node.getX());
            int y = world_to_pixel_y(node.getY());

            if(x < MAP_X - DIAMETER || x >= MAP_X + MAP_WIDTH + DIAMETER || y < MAP_Y - DIAMETER || y >= MAP_Y + MAP_HEIGHT + DIAMETER) {
                return;
            }

            double dx = mouseX - x, dy = mouseY - y;
            if(lmbDown && dx * dx + dy * dy < RADIUS * RADIUS) {
                handleNodeClick(id);
            }

            int drawX = x - RADIUS, drawY = y - RADIUS;
            g.setColor(highlightedAttractionID == id ? Color.BLUE : Color.BLACK);
            g.fillOval(drawX, drawY, DIAMETER, DIAMETER);
            g.setColor(Color.WHITE);
            g.fillOval(drawX + 3, drawY + 3, DIAMETER - 6, DIAMETER - 6);
        });
    }

    private void drawPath(Graphics2D g) {
        if(uiState == UIState.SHOW_PATH) {
            g.setColor(Color.BLUE);

            float width = 3f;
            BasicStroke stroke = new BasicStroke(width);
            g.setStroke(stroke);

            for (int i = 0; i < path.size() - 1; i++) {
                Point.Double p0 = path.get(i), p1 = path.get(i + 1);
                g.drawLine(world_to_pixel_x(p0.x), world_to_pixel_y(p0.y), world_to_pixel_x(p1.x), world_to_pixel_y(p1.y));
            }
        }
    }

    public void startView() {
        this.setFocusable(true);
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.addMouseWheelListener(this);

        this.setBackground(Color.BLACK);
        this.setLayout(null);

        final int ENTRIES_PANEL_MARGIN = 15;
        final int ENTRIES_PANEL_W = MAP_X - ENTRIES_PANEL_MARGIN * 2;
        final int ENTRIES_PANEL_H = MAP_HEIGHT - ENTRIES_PANEL_MARGIN;

        this.attractionsPanel = new JPanel(new BorderLayout());
        this.attractionsPanel.setBounds(ENTRIES_PANEL_MARGIN, ENTRIES_PANEL_MARGIN, ENTRIES_PANEL_W, ENTRIES_PANEL_H);
        this.attractionsPanel.setBackground(Color.WHITE);

        routeButton.setBounds(ENTRIES_PANEL_MARGIN, ENTRIES_PANEL_H + ENTRIES_PANEL_MARGIN * 2, ENTRIES_PANEL_W, WINDOW_HEIGHT - ENTRIES_PANEL_H - ENTRIES_PANEL_MARGIN * 5);
        routeButton.addActionListener(e -> handleRouteButton());
        this.add(routeButton);

        JPanel attractionsButtonPanel = new JPanel(null);
        attractionsButtonPanel.setLayout(new BoxLayout(attractionsButtonPanel, BoxLayout.Y_AXIS));
        controller.forEachAttraction((id, node) -> {
            JButton button = new JButton(node.getName());
            button.setAlignmentX(Component.LEFT_ALIGNMENT);
            button.setPreferredSize(new Dimension(200, 60));
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

            button.addActionListener(e -> {
                if(uiState != UIState.SELECT_POINT_B) {
                    highlightAttraction(id);
                }
                repaint();
            });

            this.attractionButtons.add(button);
            attractionsButtonPanel.add(button);
        });

        JScrollPane scrollPane = new JScrollPane(attractionsButtonPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        this.attractionsPanel.add(scrollPane, BorderLayout.CENTER);
        this.add(this.attractionsPanel);

        final int INFO_PANEL_WIDTH = (MAP_WIDTH - ENTRIES_PANEL_MARGIN * 2) / 2;

        JPanel routeInfoPanel = new JPanel(null);
        routeInfoPanel.setLayout(new BoxLayout(routeInfoPanel, BoxLayout.Y_AXIS));
        routeInfoPanel.setBounds(MAP_X, MAP_HEIGHT + ENTRIES_PANEL_MARGIN, INFO_PANEL_WIDTH, WINDOW_HEIGHT - ENTRIES_PANEL_H - ENTRIES_PANEL_MARGIN * 5);
        routeInfoPanel.setBackground(Color.WHITE);

        JLabel routeEtaTitle = new JLabel("ETA:");
        JLabel routeDistTitle = new JLabel("Distance:");
        routeInfoPanel.add(routeEtaTitle);
        routeInfoPanel.add(routeEtaLabel);
        routeInfoPanel.add(routeDistTitle);
        routeInfoPanel.add(routeDistLabel);

        Font infoPanelFont = new Font("Arial", Font.PLAIN, routeInfoPanel.getHeight() / 6);
        routeEtaTitle.setFont(infoPanelFont);
        routeEtaLabel.setFont(infoPanelFont);
        routeDistTitle.setFont(infoPanelFont);
        routeDistLabel.setFont(infoPanelFont);
        this.add(routeInfoPanel);

        attractionInfoLabel.setFont(new Font("Arial", Font.PLAIN, 20));
        attractionInfoLabel.setLineWrap(true);
        attractionInfoLabel.setWrapStyleWord(true);
        attractionInfoLabel.setEditable(false);
        JScrollPane attractionInfoPanel = new JScrollPane(attractionInfoLabel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        attractionInfoPanel.setBounds(MAP_X + INFO_PANEL_WIDTH + ENTRIES_PANEL_MARGIN, MAP_HEIGHT + ENTRIES_PANEL_MARGIN, INFO_PANEL_WIDTH, WINDOW_HEIGHT - ENTRIES_PANEL_H - ENTRIES_PANEL_MARGIN * 5);
        attractionInfoPanel.setBackground(Color.WHITE);
        this.add(attractionInfoPanel);

        JButton apiLinkButton = new JButton("Powered by QueueTimes");
        apiLinkButton.setContentAreaFilled(false);
        apiLinkButton.setBorderPainted(false);
        apiLinkButton.setFocusPainted(false);
        apiLinkButton.setOpaque(false);
        apiLinkButton.setForeground(Color.BLUE);
        apiLinkButton.setBounds(WINDOW_WIDTH - apiLinkButton.getPreferredSize().width, WINDOW_HEIGHT - 50, apiLinkButton.getPreferredSize().width, 30);
        apiLinkButton.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI("https://queue-times.com/"));
            } catch(URISyntaxException | IOException ex) {
                ex.printStackTrace();
            }
        });
        this.add(apiLinkButton);

        JFrame frame = new JFrame("Le epic map");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.add(this);
        frame.setVisible(true);
        frame.setResizable(false);

        unhighlightAttraction();
    }

    public static void main(String[] args) {
        MapView mapView = new MapView("map.jpeg", "nodes.txt");
        mapView.startView();
    }
}
