package src.main.java.org.DisneylandMap;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
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

public class MapView extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener {
    private static final int WINDOW_BOUNDS_INCREASE = 30;
    public static final Dimension DISPLAY_DIMENSIONS = Toolkit.getDefaultToolkit().getScreenSize();
    public static final int WINDOW_HEIGHT = (int)DISPLAY_DIMENSIONS.getHeight() - WINDOW_BOUNDS_INCREASE - 50;
    public static final int WINDOW_WIDTH = WINDOW_HEIGHT - WINDOW_BOUNDS_INCREASE;

    public static final int MAP_WIDTH = (int)(WINDOW_WIDTH / 1.5);
    public static final int MAP_HEIGHT = (int)(WINDOW_HEIGHT / 1.5);
    public static final int MAP_X = WINDOW_HEIGHT - MAP_HEIGHT;
    public static final int MAP_Y = 0;

    public static final int ATTRACTION_DIAMETER = 20, ATTRACTION_RADIUS = ATTRACTION_DIAMETER / 2;

    public static final Color GREEN_RGB = new Color(149, 201, 61);
    public static final Color BLUE_RGB = new Color(37, 150, 190);
    public static final Color GREY_RGB = new Color(235, 235, 235);

    private MapController controller;

    private JPanel attractionsPanel = new JPanel();
    private ArrayList<JButton> attractionButtons = new ArrayList<>();
    private JLabel routeEtaLabel = new JLabel();
    private JLabel routeDistLabel = new JLabel();
    private JTextArea attractionInfoLabel = new JTextArea();
    private JButton routeButton = new JButton();

    public MapView() {
        controller = new MapController(this);
    }

    public void updateRouteButton(Color background_color, Color foreground_color, String text) {
        routeButton.setBackground(background_color);
        routeButton.setForeground(foreground_color);
        routeButton.setText(text);
    }

    public void updateRouteLabels(String eta, String numSteps) {
        routeEtaLabel.setText(eta);
        routeDistLabel.setText(numSteps);
    }

    public void mousePressed(MouseEvent e) {
        controller.handleMousePress(e, this);
    }

    public void mouseReleased(MouseEvent e) {
        controller.handleMouseRelease();
    }

    public void mouseDragged(MouseEvent e) {
        controller.handleMouseMove(e);
        controller.handleMouseDrag(this);
    }

    public void mouseMoved(MouseEvent e) {
        controller.handleMouseMove(e);
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        controller.handleMouseWheelMove(e, this);
    }

    public void mouseClicked(MouseEvent e) { }
    public void mouseExited(MouseEvent e) { }
    public void mouseEntered(MouseEvent e) { }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D)g;

        g2.drawImage(controller.getMapImage(), controller.worldToPixelX(-.5), controller.worldToPixelY(-.5), (int)(MAP_WIDTH * controller.getMapScale()), (int)(MAP_HEIGHT * controller.getMapScale()), null);

        g2.setColor(BLUE_RGB);
        float width = 3f;
        BasicStroke stroke = new BasicStroke(width);
        g2.setStroke(stroke);
        controller.forEachPathPoint((x0, y0, x1, y1) -> {
            g.drawLine(x0, y0, x1, y1);
        });

        controller.forEachAttraction(this, (x, y, id, name, highlighted) -> {
            int radius = (int)(ATTRACTION_RADIUS * controller.getMapScale());
            int diameter = (int)(ATTRACTION_DIAMETER * controller.getMapScale());
            int drawX = x - radius, drawY = y - radius;
            g2.setColor(highlighted ? BLUE_RGB : Color.BLACK);
            g2.fillOval(drawX, drawY, diameter, diameter);
            g2.setColor(Color.WHITE);
            g2.fillOval(drawX + 3, drawY + 3, diameter - 6, diameter - 6);
        });

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, MAP_X, WINDOW_WIDTH + WINDOW_BOUNDS_INCREASE);

        g.setColor(Color.WHITE);
        g.fillRect(0, MAP_HEIGHT, WINDOW_WIDTH + WINDOW_BOUNDS_INCREASE, WINDOW_HEIGHT - MAP_HEIGHT + WINDOW_BOUNDS_INCREASE);
    }

    public void startView() {
        this.setFocusable(true);
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.addMouseWheelListener(this);

        this.setBackground(Color.BLACK);
        this.setLayout(null);

        final int ENTRIES_PANEL_MARGIN = 15;
        final int ENTRIES_PANEL_W = MapView.MAP_X - ENTRIES_PANEL_MARGIN * 2;
        final int ENTRIES_PANEL_H = MapView.MAP_HEIGHT - ENTRIES_PANEL_MARGIN;

        this.attractionsPanel = new JPanel(new BorderLayout());
        this.attractionsPanel.setBounds(ENTRIES_PANEL_MARGIN, ENTRIES_PANEL_MARGIN, ENTRIES_PANEL_W, ENTRIES_PANEL_H);
        this.attractionsPanel.setBackground(GREEN_RGB);

        routeButton.setBounds(ENTRIES_PANEL_MARGIN, ENTRIES_PANEL_H + ENTRIES_PANEL_MARGIN * 2, ENTRIES_PANEL_W, WINDOW_HEIGHT - ENTRIES_PANEL_H - ENTRIES_PANEL_MARGIN * 5);
        routeButton.addActionListener(e -> controller.handleRouteButton(this));
        this.add(routeButton);

        JPanel attractionsButtonPanel = new JPanel(null);
        attractionsButtonPanel.setLayout(new BoxLayout(attractionsButtonPanel, BoxLayout.Y_AXIS));
        controller.forEachAttraction(this, (x, y, id, name, highlighted) -> {
            JButton button = new JButton(name);
            button.setAlignmentX(Component.LEFT_ALIGNMENT);
            button.setPreferredSize(new Dimension(200, 60));
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
            button.setBackground(GREEN_RGB);
            button.setForeground(Color.WHITE);

            button.addActionListener(e -> {
                controller.handleAttractionClick(this, id);
                repaint();
            });

            this.attractionButtons.add(button);
            attractionsButtonPanel.add(button);
        });

        JScrollPane scrollPane = new JScrollPane(attractionsButtonPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        this.attractionsPanel.add(scrollPane, BorderLayout.CENTER);
        this.add(this.attractionsPanel);

        final int INFO_PANEL_WIDTH = (MapView.MAP_WIDTH - ENTRIES_PANEL_MARGIN * 2) / 2;

        JPanel routeInfoPanel = new JPanel(null);
        routeInfoPanel.setLayout(new BoxLayout(routeInfoPanel, BoxLayout.Y_AXIS));
        routeInfoPanel.setBounds(MapView.MAP_X, MapView.MAP_HEIGHT + ENTRIES_PANEL_MARGIN, INFO_PANEL_WIDTH, WINDOW_HEIGHT - ENTRIES_PANEL_H - ENTRIES_PANEL_MARGIN * 5);
        routeInfoPanel.setBackground(GREY_RGB);

        JLabel routeEtaTitle = new JLabel("ETA:");
        JLabel routeDistTitle = new JLabel("Distance:");
        routeInfoPanel.add(routeEtaTitle);
        routeInfoPanel.add(routeEtaLabel);
        routeInfoPanel.add(routeDistTitle);
        routeInfoPanel.add(routeDistLabel);

        Font infoPanelFont = new Font("Arial", Font.PLAIN, routeInfoPanel.getHeight() / 8);
        routeEtaTitle.setFont(infoPanelFont);
        routeEtaLabel.setFont(infoPanelFont);
        routeDistTitle.setFont(infoPanelFont);
        routeDistLabel.setFont(infoPanelFont);
        this.add(routeInfoPanel);

        attractionInfoLabel.setFont(new Font("Arial", Font.PLAIN, 20));
        attractionInfoLabel.setLineWrap(true);
        attractionInfoLabel.setWrapStyleWord(true);
        attractionInfoLabel.setEditable(false);
        attractionInfoLabel.setBackground(GREY_RGB);
        JScrollPane attractionInfoPanel = new JScrollPane(attractionInfoLabel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        attractionInfoPanel.setBounds(MapView.MAP_X + INFO_PANEL_WIDTH + ENTRIES_PANEL_MARGIN, MapView.MAP_HEIGHT + ENTRIES_PANEL_MARGIN, INFO_PANEL_WIDTH, WINDOW_HEIGHT - ENTRIES_PANEL_H - ENTRIES_PANEL_MARGIN * 5);
        attractionInfoPanel.setBackground(GREY_RGB);
        this.add(attractionInfoPanel);

        JButton apiLinkButton = new JButton("Powered by Queue-Times.com");
        apiLinkButton.setContentAreaFilled(false);
        apiLinkButton.setBorderPainted(false);
        apiLinkButton.setFocusPainted(false);
        apiLinkButton.setOpaque(false);
        apiLinkButton.setForeground(BLUE_RGB);
        apiLinkButton.setBounds(WINDOW_WIDTH - apiLinkButton.getPreferredSize().width, WINDOW_HEIGHT - 50, apiLinkButton.getPreferredSize().width, 30);
        apiLinkButton.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI("https://queue-times.com/en-US"));
            } catch(URISyntaxException | IOException ex) {
                ex.printStackTrace();
            }
            apiLinkButton.setForeground(Color.BLACK);
        });
        this.add(apiLinkButton);

        int heightIncrease = System.getProperty("os.name").startsWith("Windows") ? WINDOW_BOUNDS_INCREASE : 0;
        JFrame frame = new JFrame("Disneyland Route Map App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(WINDOW_WIDTH + heightIncrease, WINDOW_HEIGHT + heightIncrease);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.add(this);
        frame.setVisible(true);
        frame.setResizable(false);

        controller.unhighlightAttraction(this);
    }

    public void setAttractionInfo(String text) {
        attractionInfoLabel.setText(text);
        attractionInfoLabel.setCaretPosition(0);
    }

    public int getAttractionRadius() {
        return ATTRACTION_RADIUS;
    }

    public static void main(String[] args) {
        MapView mapView = new MapView();
        mapView.startView();
    }
}
