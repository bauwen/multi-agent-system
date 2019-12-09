package main;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {

    public static String AGENT_TYPE = "complex";  // "simple" or "naive" or "complex"
    public static boolean DEBUG = false;

    public static int parcelAmount = 100;
    public static int arrivalWindow = 15000;
    public static int chargingMultiplier = 1;
    public static int batteryCapacity = 200;

    public static int simpleDeliveryCount = 1;

    public static Simulation simulation;
    public static int cellSize = 32; //28;
    public static int gridWidth = 23; //26;
    public static int gridHeight = 16; //19;

    public static int timesCharged = 0;
    public static int nbOnTime = 0;
    public static int nbTooLate = 0;
    public static int avgLateness = 0;

    public static int scenarioNumber = -1;  // -1 if no automatic scenario

    public static void setupSimulation() {
        // only if scenario number < 0

        Tracker.init();
        loadMap("warehouse.mpm");
        //simulation.setupParcelEvents();
    }

    public static main.CanvasWindow window = null;
    public static int defaultSimulationSpeed = 60;
    public static double speedMultiplier = 1;
    public static String editorObject = "wall";
    public static int gridOffsetX = 300;
    public static int gridOffsetY = 50;

    public static void main(String[] args) {
        try {
            int x = Integer.parseInt(args[0]);
            switch (x) {
                case 1:
                    AGENT_TYPE = "simple";
                    simpleDeliveryCount = 1;
                    break;
                case 2:
                    AGENT_TYPE = "simple";
                    simpleDeliveryCount = 2;
                    break;
                case 3:
                    AGENT_TYPE = "naive";
                    break;
                default:
                    AGENT_TYPE = "complex";
                    break;
            }
        }
        catch (Exception e){
            System.out.println("Default (complex) agent type (1 = simple1, 2 = simple2, 3 = naive, 4 = complex)");
        }

        main.CanvasWindow.create(Main::init);
    }

    public static void init() {
        String title = "MAS Project - " + AGENT_TYPE;
        if (AGENT_TYPE.equals("simple")) {
            title += "" + simpleDeliveryCount;
        }
        window = new main.CanvasWindow(1050, 580, title);
        setupGUI();
        Graph graph = new Graph(gridHeight, gridWidth, cellSize);
        simulation = new Simulation(graph);
        window.addPainter(simulation);
        setupSimulation();
    }

    public static Agent createAgent(int x, int y) {
        switch (AGENT_TYPE) {
            case "simple":
                return new AgentSimple(x, y);
            case "naive":
                return new AgentNaive(x, y);
            case "complex":
                return new AgentComplex(x, y);
        }

        return null;
    }

    public static boolean flag = false;
    public static String loadedMap = "";

    public static void restartSimulation() {
        if (simulation != null) {
            simulation.stop();
            window.buttonStart.setText("Start");
            window.labelTick.setText("0");
        }

        String contents = "";
        if (!loadedMap.equals("")) {
            try {
                byte[] encoded = Files.readAllBytes(Paths.get(loadedMap));
                contents = new String(encoded, StandardCharsets.UTF_8);
            } catch (IOException e) {
                // do nothing
            }
        } else if (scenarioNumber < 0) {
            contents = getMapContents();
        }

        setMapContents(contents, true);

        nbOnTime = 0;
        nbTooLate = 0;
        avgLateness = 0;
        updateStats();
    }

    public static void updateStats() {
        int totalDelivered = nbOnTime + nbTooLate;

        window.labelValStat1.setText("" + totalDelivered);
        window.labelValStat2.setText("" + nbOnTime);
        window.labelValStat3.setText("" + nbTooLate);

        if (totalDelivered == 0) {
            window.labelValStat4.setText("0");
        } else {
            window.labelValStat4.setText("" + (Math.floor(avgLateness / totalDelivered * 100) / 100));
        }
    }

    public static void setupGUI() {
        window.buttonStart.addListener(() -> {
            if (simulation.isRunning()) {
                simulation.stop();
                window.buttonStart.setText("Start");
                //System.out.println("Average parcel time: " + Tracker.getAverageParcelTime());
            } else {
                simulation.setupParcelEvents();
                simulation.start((int) (speedMultiplier * defaultSimulationSpeed));
                window.buttonStart.setText("Stop");
            }
        });

        window.buttonStep.addListener(() -> {
            if (!simulation.isRunning()) {
                int n = (int) Math.ceil(speedMultiplier);

                for (int i = 0; i < n; i++) {
                    simulation.simulate();
                }
            }
        });

        window.buttonRestart.addListener(() -> {
            restartSimulation();
        });

        window.buttonSpeedFaster.addListener(() -> {
            if (speedMultiplier < 32) {
                speedMultiplier *= 2;

                String s = String.format("%s", speedMultiplier);
                if (speedMultiplier == (int) speedMultiplier) {
                    s = String.format("%d", (int) speedMultiplier);
                }
                window.labelRate.setText("x" + s);

                if (simulation.isRunning()) {
                    simulation.stop();
                    simulation.start((int) (speedMultiplier * defaultSimulationSpeed));
                }
            }
        });

        window.buttonSpeedSlower.addListener(() -> {
            if (speedMultiplier > 0.25) {
                speedMultiplier /= 2;

                String s = String.format("%s", speedMultiplier);
                if (speedMultiplier == (int) speedMultiplier) {
                    s = String.format("%d", (int) speedMultiplier);
                }
                window.labelRate.setText("x" + s);

                if (simulation.isRunning()) {
                    simulation.stop();
                    simulation.start((int) (speedMultiplier * defaultSimulationSpeed));
                }
            }
        });

        window.buttonAddWall.addListener(() -> {
            editorObject = "wall";
            window.labelObject.setText(editorObject);
        });

        window.buttonAddBattery.addListener(() -> {
            editorObject = "battery";
            window.labelObject.setText(editorObject);
        });

        window.buttonAddDelivery.addListener(() -> {
            editorObject = "delivery";
            window.labelObject.setText(editorObject);
        });

        window.buttonAddAgent.addListener(() -> {
            editorObject = "agent";
            window.labelObject.setText(editorObject);
        });

        window.buttonLoadMap.addListener(() -> {
            if (simulation.getPassedTime() > 0) {
                return;
            }

            if (flag) return;
            flag = true;

            JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
            chooser.setFileFilter(new FileNameExtensionFilter("MAS Project Maps (*.mpm)", "mpm"));
            int returnVal = chooser.showOpenDialog(window.frame);
            if(returnVal == JFileChooser.APPROVE_OPTION) {
                String filename = chooser.getSelectedFile().getAbsolutePath();
                if (!filename.endsWith(".mpm")) filename += ".mpm";

                try {
                    byte[] encoded = Files.readAllBytes(Paths.get(filename));
                    String contents = new String(encoded, StandardCharsets.UTF_8);
                    setMapContents(contents, true);
                    loadedMap = filename;
                } catch (IOException e) {
                    // do nothing
                }

                flag = false;
            } else {
                flag = false;
            }
        });

        window.buttonSaveMap.addListener(() -> {
            if (simulation.getPassedTime() > 0) {
                return;
            }

            if (flag) return;
            flag = true;

            JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
            chooser.setFileFilter(new FileNameExtensionFilter("MAS Project Maps (*.mpm)", "mpm"));
            int returnVal = chooser.showSaveDialog(window.frame);
            if(returnVal == JFileChooser.APPROVE_OPTION) {
                String filename = chooser.getSelectedFile().getAbsolutePath();

                if (!filename.endsWith(".mpm")) filename += ".mpm";

                try {
                    FileWriter fw = new FileWriter(filename);
                    fw.write(getMapContents());
                    fw.close();
                } catch (IOException e) {
                    // do nothing
                }

                flag = false;
            } else {
                flag = false;
            }
        });
    }

    public static String getMapContents() {
        StringBuilder sb = new StringBuilder();

        Graph grid = simulation.getGraph();
        for (int col = 0; col < grid.getWidth(); col++) {
            for (int row = 0; row < grid.getHeight(); row++) {
                if (!grid.hasNodeAt(row, col)) {
                    sb.append("wall " + row + " " + col + "\n");
                }
            }
        }

        for (SimulationObject object : simulation.getObjects()) {
            boolean isStation = object instanceof BatteryStation;
            boolean isLoc = object instanceof DeliveryLocation;
            boolean isAgent = object instanceof Agent;

            if (!isStation && !isLoc && !isAgent) {
                continue;
            }

            String name = isStation ? "battery" : isLoc ? "delivery" : "agent";
            Point point = object.getPosition();

            if (name.equals("agent")) {
                point = ((Agent) object).getStartPosition();
            }

            sb.append(name + " " + point.getX() + " " + point.getY() + "\n");
        }

        return sb.toString();
    }

    public static void setMapContents(String contents) {
        setMapContents(contents, false);
    }

    public static void setMapContents(String contents, boolean reset) {
        //int simulationNumber = scenarioNumber;
        boolean done = false;
        if (reset && simulation != null) {
            //simulationNumber = -1;
            window.removePainter(simulation);
            simulation = new Simulation(new Graph(gridHeight, gridWidth, cellSize));
            window.addPainter(simulation);
            done = true;
        }

        String[] lines = contents.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.equals("")) continue;
            String[] tokens = line.split(" ");

            String name = tokens[0];
            int x = Integer.parseInt(tokens[1]);
            int y = Integer.parseInt(tokens[2].trim());

            switch (name) {
                case "wall":
                    simulation.getGraph().setNodeAt(x, y, false);
                    break;

                case "battery":
                    simulation.register(new BatteryStation(x, y));
                    break;

                case "delivery":
                    simulation.register(new DeliveryLocation(x, y));
                    break;

                case "agent":
                    simulation.register(createAgent(x, y));
                    break;
            }
        }

        if (done) {
            //simulation.setupParcelEvents();
        }
        //setupSimulation();
    }

    private static void loadMap(String filename) {
        String path = Paths.get(".").toAbsolutePath().normalize().toString() + "\\" + filename;

        try {
            byte[] encoded = Files.readAllBytes(Paths.get(path));
            String contents = new String(encoded, StandardCharsets.UTF_8);
            Main.setMapContents(contents);
        } catch (IOException e) {
            // do nothing
        }

    }

    public static void onClick(int x, int y) {
        Graph graph = simulation.getGraph();
        int cellSize = graph.getCellSize();
        int width = graph.getWidth() * cellSize;
        int height = graph.getHeight() * cellSize;
        int ox = gridOffsetX - cellSize / 2;
        int oy = gridOffsetY - cellSize / 2;

        if (x < ox || ox + width <= x || y < oy || oy + height <= y) {
            return;
        }

        int col = (x - ox) / cellSize;
        int row = (y - oy) / cellSize;
        int px = col * cellSize;
        int py = row * cellSize;

        if (simulation.getPassedTime() > 0) {
            Parcel parcel = simulation.getParcelAt(new Point(px, py));
            if (parcel != null) {
                simulation.unregister(parcel);
            } else {
                simulation.register(new Parcel(new Point(px, py), simulation.getRandomDeliveryLocation(), simulation.getPassedTime()));
            }
            return;
        }

        if (!graph.hasNodeAt(row, col)) {
            editorObject = "wall";
        }
        Point point = new Point(px, py);
        if (simulation.getBatteryStationAt(point) != null) {
            editorObject = "battery";
        }
        if (simulation.getDeliveryLocationAt(point) != null) {
            editorObject = "delivery";
        }
        if (simulation.getAgentAt(point) != null) {
            editorObject = "agent";
        }

        switch (editorObject) {
            case "wall":
                graph.setNodeAt(row, col, !graph.hasNodeAt(row, col));
                break;

            case "battery":
                BatteryStation station = simulation.getBatteryStationAt(new Point(px, py));
                if (station != null) {
                    simulation.unregister(station);
                } else {
                    simulation.register(new BatteryStation(px, py));
                }
                break;

            case "delivery":
                DeliveryLocation loc = simulation.getDeliveryLocationAt(new Point(px, py));
                if (loc != null) {
                    simulation.unregister(loc);
                } else {
                    simulation.register(new DeliveryLocation(px, py));
                }
                break;

            case "agent":
                Agent agent = simulation.getAgentAt(new Point(px, py));
                if (agent != null) {
                    simulation.unregister(agent);
                    simulation.getNodeAt(point).setBlocked(false);
                } else {
                    simulation.register(createAgent(px, py));
                }
                break;
        }

        window.labelObject.setText(editorObject);
    }
}
