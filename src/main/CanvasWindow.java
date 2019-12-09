package main;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.swing.*;

public class CanvasWindow {

    public static void create(Runnable callback) {
        java.awt.EventQueue.invokeLater(callback);
    }

    private int width;
    private int height;
    private String title;
    private Panel panel;
    public Frame frame;
    private Set<Painter> painters = new HashSet<>();

    private int mouseX = 0;
    private int mouseY = 0;
    private boolean mouseLeftPressed = false;
    private boolean mouseLeftReleased = false;
    private boolean mouseLeftDown = false;

    protected CanvasWindow(int width, int height, String title) {
        this.width = width;
        this.height = height;
        this.title = title;
        this.show();
    }

    public void addPainter(Painter painter) {
        this.painters.add(painter);
    }

    public void removePainter(Painter painter) {
        this.painters.remove(painter);
    }

    /**
     * Call this method if the canvas is out of date and needs to be repainted.
     * This will cause method {@link #paint(Graphics)} to be called after the current call of method handleMouseEvent or handleKeyEvent finishes.
     */
    protected final void repaint() {
        if (panel != null) {
            panel.repaint();
        }
    }

    /**
     * Called to allow you to paint on the canvas.
     *
     * You should not use the Graphics object after you return from this method.
     *
     * @param g This object offers the methods that allow you to paint on the canvas.
     */
    protected void paint(Graphics g) {
        paintGUI(g);
        for (Painter painter : this.painters) {
            painter.paint(g);
        }
    }

    /**
     * Called when the user presses (e.getID() == MouseEvent.MOUSE_PRESSED), releases (e.getID() == MouseEvent.MOUSE_RELEASED), or drags (e.getID() == MouseEvent.MOUSE_DRAGGED) the mouse.
     *
     * @param e Details about the event
     */
    protected void handleMouseEvent(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();

        switch (e.getID()) {
            case MouseEvent.MOUSE_MOVED:
                this.mouseX = x;
                this.mouseY = y;
                break;
            case MouseEvent.MOUSE_PRESSED:
                if (!mouseLeftDown) {
                    mouseLeftDown = true;
                    mouseLeftPressed = true;
                    Main.onClick(x, y);
                }
                break;
            case MouseEvent.MOUSE_RELEASED:
                if (mouseLeftDown) {
                    mouseLeftDown = false;
                    mouseLeftReleased = true;
                }
                break;
        }
    }

    /**
     * Called when the user presses a key (e.getID() == KeyEvent.KEY_PRESSED) or enters a character (e.getID() == KeyEvent.KEY_TYPED).
     *
     * @param e
     * 		The key event.
     */
    protected void handleKeyEvent(KeyEvent e) {
    }

    class Panel extends JPanel {
        private static final long serialVersionUID = 1L;

        {
            setPreferredSize(new Dimension(width, height));
            setBackground(Color.WHITE);
            setFocusable(true);

            addMouseListener(new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    handleMouseEvent(e);
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    handleMouseEvent(e);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    handleMouseEvent(e);
                }

            });

            addMouseMotionListener(new MouseAdapter() {

                @Override
                public void mouseDragged(MouseEvent e) {
                    handleMouseEvent(e);
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    handleMouseEvent(e);
                }

            });

            addKeyListener(new KeyAdapter() {

                @Override
                public void keyTyped(KeyEvent e) {
                    handleKeyEvent(e);
                }

                @Override
                public void keyPressed(KeyEvent e) {
                    handleKeyEvent(e);
                }

                @Override
                public void keyReleased(KeyEvent e) {
                    handleKeyEvent(e);
                }

            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            CanvasWindow.this.paint(g);
        }

    }


    private class Frame extends JFrame {
        private static final long serialVersionUID = 1L;

        Frame(String title) {
            super(title);

            addWindowListener(new WindowAdapter() {

                @Override
                public void windowClosed(WindowEvent e) {
                    System.exit(0);
                }

            });
            getContentPane().add(panel);
            pack();
            setLocationRelativeTo(null);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        }
    }

    public final void show() {
        if (!EventQueue.isDispatchThread()) {
            throw new RuntimeException("You must call this method from the AWT dispatch thread");
        }

        panel = new Panel();
        frame = new Frame(title);
        frame.setVisible(true);

        final CanvasWindow cw = this;

        Thread loop = new Thread(() -> {
            while (true) {
                cw.repaint();

                try {
                    Thread.sleep(1000 / 60);
                } catch (InterruptedException e) {
                    // do nothing
                }
            }
        });
        loop.start();
        //new Timer(1000 / 60, (ActionEvent e) -> this.repaint()).start();
    }


    // GUI

    public GUILabel labelSim = new GUILabel("Simulation:", 20, 40, 18);
    public GUILabel labelTick = new GUILabel("0", 170, 40, 16);
    public GUIButton buttonStart = new GUIButton("Start", 20, 60, 100);
    public GUIButton buttonStep = new GUIButton("Step", 130, 60, 100);
    public GUIButton buttonRestart = new GUIButton("Restart", 20, 100, 100);

    public GUILabel labelSpeed = new GUILabel("Speed:", 20, 170, 18);
    public GUILabel labelRate = new GUILabel("x1", 170, 170, 16);
    public GUIButton buttonSpeedSlower = new GUIButton("Slower", 20, 190, 100);
    public GUIButton buttonSpeedFaster = new GUIButton("Faster", 130, 190, 100);

    public GUILabel labelEditor = new GUILabel("Editor:", 20, 260, 18);
    public GUILabel labelObject = new GUILabel("wall", 170, 260, 14);
    public GUIButton buttonAddWall = new GUIButton("Wall", 20, 280, 100);
    public GUIButton buttonAddBattery = new GUIButton("Battery", 130, 280, 100);
    public GUIButton buttonAddDelivery = new GUIButton("Delivery", 20, 320, 100);
    public GUIButton buttonAddAgent = new GUIButton("Agent", 130, 320, 100);

    public GUIButton buttonLoadMap = new GUIButton("Load Map..", 20, 360, 100);
    public GUIButton buttonSaveMap = new GUIButton("Save Map..", 130, 360, 100);

    public GUILabel labelStats = new GUILabel("Data:", 20, 440, 18);

    //public GUILabel labelTell1 = new GUILabel("[Statistieken over", 50, 480, 14);
    //public GUILabel labelTell2 = new GUILabel("de huidige simulatie]", 50, 510, 14);

    public GUILabel labelTextStat1 = new GUILabel("#parcels delivered: ", 30, 480, 12);
    public GUILabel labelTextStat2 = new GUILabel("#parcels on time: ", 30, 500, 12);
    public GUILabel labelTextStat3 = new GUILabel("#parcels too late: ", 30, 520, 12);
    public GUILabel labelTextStat4 = new GUILabel("average lateness: ", 30, 540, 12);

    public GUILabel labelValStat1 = new GUILabel("0", 160, 480, 12);
    public GUILabel labelValStat2 = new GUILabel("0", 160, 500, 12);
    public GUILabel labelValStat3 = new GUILabel("0", 160, 520, 12);
    public GUILabel labelValStat4 = new GUILabel("0", 160, 540, 12);

    private ArrayList<GUIElement> buttons = new ArrayList(){{
        add(labelSim);
        add(labelTick);
        add(buttonRestart);
        add(buttonStart);
        add(buttonStep);

        add(labelSpeed);
        add(labelRate);
        add(buttonSpeedSlower);
        add(buttonSpeedFaster);

        add(labelEditor);
        add(labelObject);
        add(buttonAddWall);
        add(buttonAddBattery);
        add(buttonAddDelivery);
        add(buttonAddAgent);
        add(buttonLoadMap);
        add(buttonSaveMap);

        add(labelStats);
        //add(labelTell1);
        //add(labelTell2);
        add(labelTextStat1);
        add(labelTextStat2);
        add(labelTextStat3);
        //add(labelTextStat4);
        add(labelValStat1);
        add(labelValStat2);
        add(labelValStat3);
        //add(labelValStat4);
    }};
    private static Font buttonFont = new Font("Verdana", Font.PLAIN, 15);

    public void paintGUI(Graphics g) {
        int pw = 250;

        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(0, 0, pw + 1, this.height);

        g.setColor(Color.GRAY);
        g.fillRect(pw + 1, 0, this.width - pw, this.height);

        g.setColor(Color.WHITE);
        int ox = Main.gridOffsetX - Main.cellSize / 2;
        int oy = Main.gridOffsetY - Main.cellSize / 2;
        int ow = Main.gridWidth * Main.cellSize;
        int oh = Main.gridHeight * Main.cellSize;
        g.fillRect(ox, oy, ow, oh);

        g.setColor(Color.BLACK);
        g.drawRect(1, 1, pw, this.height - 2);

        for (GUIElement button : buttons) {
            g.setColor(Color.BLACK);
            button.paint(g, this.mouseX, this.mouseY, this.mouseLeftPressed);
        }

        this.mouseLeftPressed = false;
        this.mouseLeftReleased = false;
    }

    public abstract class GUIElement {
        public abstract void paint(Graphics g, int mouseX, int mouseY, boolean clicked);
    }

    public class GUILabel extends GUIElement {

        private String text;
        private int x;
        private int y;
        private Font font;

        public GUILabel(String text, int x, int y, int size) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.font = new Font("Verdana", Font.PLAIN, size);
        }

        public void setText(String text) {
            this.text = text;
        }

        @Override
        public void paint(Graphics g, int mouseX, int mouseY, boolean clicked) {
            Font font = this.font;
            g.setFont(font);
            g.drawString(this.text, x, y);
        }
    }

    public class GUIButton extends GUIElement {

        private String text;
        private int x;
        private int y;
        private int width;
        private int height = 24;
        private Set<Runnable> listeners = new HashSet<>();

        public GUIButton(String text, int x, int y, int width) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.width = width;
        }

        public void setText(String text) {
            this.text = text;
        }

        public void addListener(Runnable listener) {
            this.listeners.add(listener);
        }

        @Override
        public void paint(Graphics g, int mouseX, int mouseY, boolean clicked) {
            boolean hover = this.x <= mouseX && mouseX < this.x + this.width &&
                    this.y <= mouseY && mouseY < this.y + this.height;

            if (hover && clicked) {
                for (Runnable listener : this.listeners) {
                    listener.run();
                }
            }

            g.setColor(Color.WHITE);
            g.fillRect(this.x, this.y, this.width, this.height);

            g.setColor(Color.BLACK);
            g.drawRect(this.x, this.y, this.width, this.height);

            if (hover) {
                g.fillRect(this.x, this.y, this.width, this.height);
                g.setColor(Color.WHITE);
            }

            Font font = CanvasWindow.buttonFont;
            g.setFont(font);
            FontMetrics metrics = g.getFontMetrics(font);
            int x = this.x + this.width / 2 - metrics.stringWidth(this.text) / 2;
            int y = this.y + this.height / 2 - metrics.getHeight() / 2 + metrics.getAscent();
            g.drawString(this.text, x, y);
        }
    }
}