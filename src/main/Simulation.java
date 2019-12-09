package main;

import java.awt.*;
import java.util.*;
import java.util.List;

public class Simulation implements Painter {

    private Graph graph;
    private Set<SimulationObject> objects = new HashSet<>();
    private Thread thread;
    private Runner runner;
    private int passedTime;
    private Map<Integer, List<ParcelEvent>> eventMap = new HashMap<>();
    //private Scenario scenario = new Scenario(this);

    private boolean initiated = false;
    private Map<Point, Node> pointToNode = new HashMap<>();

    private int parcelAmount = Main.parcelAmount;//100;
    private int arrivalWindowOfParcels = Main.arrivalWindow;//15000;
    private int parcelsDelivered = 0;

    public Simulation(Graph graph) {
        this.graph = graph;
        this.createNodes();
    }

    public void setupParcelEvents() {
        this.eventMap = new HashMap<>();
        Random r = new Random(192837464);
        Random q = new Random(738483);
        //int parcelAmount = 4*100;//0;
        //int arrivalWindowOfParcels = 1000;//2*50000;
        boolean arrivalVariance = false;

        for (int i = 0; i < parcelAmount; i++) {
            int arrivalTime;
            if (arrivalVariance) {
                arrivalTime = r.nextInt(arrivalWindowOfParcels);
            } else {
                arrivalTime = 100 + arrivalWindowOfParcels/parcelAmount * i;
            }

            Point p = this.getRandomFreeLocation(r);
            this.addParcelEvent(arrivalTime, new ParcelEvent(p, this.getRandomDeliveryLocationSeeded(q), arrivalTime));
        }
    }

    public void addParcelEvent(int time, ParcelEvent event) {
        if (!eventMap.containsKey(time)) {
            eventMap.put(time, new ArrayList<>());
        }

        List<ParcelEvent> events = eventMap.get(time);
        events.add(event);
    }

    public void doParcelEvents(int time, Simulation sim) {
        if (!eventMap.containsKey(time)) {
            return;
        }

        List<ParcelEvent> events = eventMap.get(time);
        for (ParcelEvent event: events) {
            Parcel parcel = new Parcel(event.from, event.to, event.arrivalTime);
            this.register(parcel);
        }
    }

    public void parcelDelivered(Parcel parcel) {
        //this.statTracker.addParcelDelivery(parcel, this.getPassedTime());
        Tracker.addParcelDelivery(parcel, this.getPassedTime());

        parcelsDelivered += 1;
        if (parcelsDelivered >= parcelAmount && Main.DEBUG) {
            System.out.println("\n-------------------");
            System.out.println("Average parcel time: " + Tracker.getAverageParcelTime());
            System.out.println("Number of times charged: " + Main.timesCharged);
        }

        int delta = this.getPassedTime() - parcel.getDeadline();
        if (delta <= 0) {
            Main.nbOnTime += 1;
        } else {
            Main.nbTooLate += 1;
        }
        Main.avgLateness += delta;
        Main.updateStats();
    }

    private void createNodes() {
        Set<Point> points = this.graph.getPoints();
        for (Point point : points) {
            Node node = new Node(point.getX(), point.getY());
            this.register(node);
            this.pointToNode.put(point, node);
        }
    }

    public void register(SimulationObject object) {
        objects.add(object);
        object.init(this, graph);
    }

    public void unregister(SimulationObject object) {
        objects.remove(object);
    }

    public boolean isRunning() {
        return this.thread != null;
    }

    public Graph getGraph() {
        return this.graph;
    }

    public int getPassedTime() {
        return this.passedTime;
    }

    public void simulate() {
        passedTime += 1;
        this.doParcelEvents(passedTime, this);
        //scenario.doEvents(passedTime, this);

        Set<SimulationObject> objects = new HashSet<>(this.objects);
        for (SimulationObject object : objects) {
            object.tick();
        }

        Main.window.labelTick.setText("" + passedTime);
    }

    public void start(int ticksPerSecond) {
        if (this.thread != null) {
            return;
        }

        final long millisecondsPerTick = 1000 / ticksPerSecond;

        this.runner = new Runner(this, millisecondsPerTick);
        this.thread = new Thread(runner);
        this.thread.start();
    }

    public void stop() {
        if (this.thread == null) {
            return;
        }

        this.runner.stop();
        this.runner = null;

        try {
            this.thread.join();
            this.thread = null;
        } catch (InterruptedException e) {
            // do nothing
        }
    }

    public Set<SimulationObject> getObjects() {
        return new HashSet<>(this.objects);
    }

    public List<BatteryStation> getBatteryStations() {
        List<BatteryStation> stations = new ArrayList<>();
        for (SimulationObject object : this.getObjects()) {
            if (!(object instanceof BatteryStation)) continue;
            BatteryStation station = (BatteryStation) object;
            stations.add(station);
        }
        return stations;
    }

    public BatteryStation getBatteryStationAt(Point point) {
        for (BatteryStation station : this.getBatteryStations()) {
            if (station.getPosition().equals(point)) {
                return station;
            }
        }
        return null;
    }

    public List<DeliveryLocation> getDeliveryLocations() {
        List<DeliveryLocation> locs = new ArrayList<>();
        for (SimulationObject object : this.getObjects()) {
            if (!(object instanceof DeliveryLocation)) continue;
            DeliveryLocation loc = (DeliveryLocation) object;
            locs.add(loc);
        }
        return locs;
    }

    public DeliveryLocation getDeliveryLocationAt(Point point) {
        for (DeliveryLocation loc : this.getDeliveryLocations()) {
            if (loc.getPosition().equals(point)) {
                return loc;
            }
        }
        return null;
    }

    public List<Agent> getAgents() {
        List<Agent> agents = new ArrayList<>();
        for (SimulationObject object : this.getObjects()) {
            if (!(object instanceof Agent)) continue;
            Agent agent = (Agent) object;
            agents.add(agent);
        }
        return agents;
    }

    public Agent getAgentAt(Point point) {
        for (Agent agent : this.getAgents()) {
            if (agent.getPosition().equals(point)) {
                return agent;
            }
        }
        return null;
    }

    public List<Parcel> getParcels() {
        List<Parcel> parcels = new ArrayList<>();
        for (SimulationObject object : this.getObjects()) {
            if (!(object instanceof Parcel)) continue;
            Parcel parcel = (Parcel) object;
            if (!parcel.isBeingTransported()) {
                parcels.add(parcel);
            }
        }
        return parcels;
    }

    public Parcel getParcelAt(Point point) {
        for (SimulationObject object : this.getObjects()) {
            if (!(object instanceof Parcel)) continue;
            Parcel parcel = (Parcel) object;
            if (parcel.getPosition().equals(point)) {
                return parcel;
            }
        }
        return null;
    }

    public Node getNodeAt(Point point) {
        return this.pointToNode.get(point);
    }

    public Point getRandomDeliveryLocation() {
        List<DeliveryLocation> locs = this.getDeliveryLocations();
        int index = (int) (Math.random() * locs.size());
        DeliveryLocation loc = locs.get(index);
        return loc.getPosition();
    }

    public Point getRandomDeliveryLocationSeeded(Random r) {
        List<DeliveryLocation> locs = this.getDeliveryLocations();
        int index = r.nextInt(locs.size());
        DeliveryLocation loc = locs.get(index);
        return loc.getPosition();
    }

    private Point getRandomFreeLocation(Random r) {
        /*
        int x, y, col, row;
        boolean cond;
        Point p;

        do {
            row = r.nextInt(Main.gridHeight);
            col = r.nextInt(Main.gridWidth);
            y = row * Main.cellSize;
            x = col * Main.cellSize;
            p = new Point(x, y);
            cond = this.getDeliveryLocationAt(p) == null && this.getBatteryStationAt(p) == null;
        } while (!this.graph.hasNodeAt(row, col) || !cond);

        return p;
        */
        while (true) {
            Point point = this.graph.getRandomPointSeeded(r);
            boolean free = true;
            for (SimulationObject object : this.objects) {
                if (object instanceof Agent || object instanceof BatteryStation || object instanceof DeliveryLocation) {
                    if (object.getPosition().equals(point)) {
                        free = false;
                        break;
                    }
                }
            }
            if (free) {
                return point;
            }
        }
    }


    // Simulation Runner (via threads)

    private class Runner implements Runnable {

        private Simulation simulation;
        private long millisecondsPerTick;
        private boolean running = true;

        public Runner(Simulation simulation, long millisecondsPerTick) {
            this.simulation = simulation;
            this.millisecondsPerTick = millisecondsPerTick;
        }

        public void stop() {
            this.running = false;
        }

        @Override
        public void run() {
            while (running) {
                simulation.simulate();

                try {
                    Thread.sleep(this.millisecondsPerTick);
                } catch (InterruptedException e) {
                    // do nothing
                }
            }
        }
    }


    // graphics painter

    @Override
    public void paint(Graphics g) {
        Set<SimulationObject> objects;
        try {
            objects = new HashSet<>(this.objects);
        } catch (ConcurrentModificationException e) {
            return;
        }

        int ox = Main.gridOffsetX;
        int oy = Main.gridOffsetY;

        // grid
        Graph grid = this.graph;
        for (int col = 0; col < grid.getWidth(); col++) {
            for (int row = 0; row < grid.getHeight(); row++) {
                int cellSize = grid.getCellSize();
                int x = ox + col * cellSize - cellSize / 2;
                int y = oy + row * cellSize - cellSize / 2;

                g.setColor(Color.BLACK);
                if (grid.hasNodeAt(row, col)) {
                    g.drawRect(x, y, cellSize, cellSize);
                    g.setColor(new Color(230, 230, 230));
                    int radius = 2;
                    int cx = x + cellSize / 2 - radius;
                    int cy = y + cellSize / 2 - radius;
                    g.fillOval(cx, cy, 2 * radius, 2 * radius);
                } else {
                    g.setColor(Color.DARK_GRAY);
                    g.fillRect(x, y, cellSize, cellSize);
                }
            }
        }

        for (SimulationObject object : objects) {
            if (object instanceof BatteryStation) {
                BatteryStation station = (BatteryStation) object;
                Point position = station.getPosition();
                int x = ox + position.getX();
                int y = oy + position.getY();

                int radius = Main.cellSize / 2 - 2;
                g.setColor(Color.BLUE);

                if (Main.DEBUG) {
                    if (station.isReserved()) {
                        g.setColor(Color.MAGENTA);
                    }
                }

                g.fillRect(x - radius, y - radius, 2 * radius + 1, 2 * radius + 1);

                g.setColor(Color.YELLOW);
                g.drawLine(x + 3, y - 5, x - 4, y);
                g.drawLine(x - 4, y, x + 4, y);
                g.drawLine(x + 4, y, x - 3, y + 6);
            }
            else if (object instanceof DeliveryLocation) {
                DeliveryLocation loc = (DeliveryLocation) object;
                Point position = loc.getPosition();
                int x = ox + position.getX();
                int y = oy + position.getY();

                int radius = Main.cellSize / 2 - 2;
                Graphics2D g2 = (Graphics2D) g;
                g2.setStroke(new BasicStroke(3));
                g.setColor(Color.MAGENTA);
                g.drawOval(x - radius, y - radius, 2 * radius, 2 * radius);
                g2.setStroke(new BasicStroke(1));
            }
            else if (object instanceof Node) {
                Node node = (Node) object;
                Point position = node.getPosition();
                int x = ox + position.getX();
                int y = oy + position.getY();
                int radius = Main.cellSize / 2 - 2;

                if (Main.DEBUG) {
                    if (node.isBlocked()) {
                        g.setColor(Color.GREEN);
                        g.fillRect(x - radius, y - radius, 2 * radius, 2 * radius);
                    }
                    if (node.isReserved(this.passedTime, this.passedTime + 1)) {
                        g.setColor(Color.CYAN);
                        g.fillRect(x - radius, y - radius, 2 * radius, 2 * radius);
                    }
                }
            }
        }

        // objects
        for (SimulationObject object : objects) {
            if (object instanceof Agent) {
                Agent agent = (Agent) object;
                Point position = agent.getPosition();
                int x = ox + position.getX();
                int y = oy + position.getY();

                int radius = 5;
                int extra = 3;
                g.setColor(new Color(255, 0, 0));

                if (Main.DEBUG) {
                    if (agent instanceof AgentSimple) {
                        g.setColor(new Color(255, 128, 128));
                    }
                    /*if (agent instanceof AgentComplex) {
                        g.setColor(new Color(128, 128, 255));
                    }*/
                    if (agent.currentBatteryStation != null) {
                        g.setColor(Color.YELLOW);
                    }
                }

                int bx = x;
                int by = y;
                if (agent.direction == 0 || agent.direction == 180) {
                    g.fillRect(x - radius - extra, y - radius, 2 * radius + 2 * extra, 2 * radius);
                    bx = agent.direction == 0 ? x + radius : x - radius;
                } else {
                    g.fillRect(x - radius, y - radius - extra, 2 * radius, 2 * radius + 2 * extra);
                    by = agent.direction == 270 ? y + radius : y - radius;
                }
                g.setColor(Color.BLACK);
                g.fillRect(bx - 1, by - 1, 2, 2);


                // battery status
                int aw = 10;
                int ax = x - aw;
                int ay = y + radius + 4;
                double p = (double) agent.batteryLevel / agent.batteryLevelFull;

                g.setColor(Color.WHITE);
                g.fillRect(ax, ay + 1, 2 * aw, 2);

                /*int red = (int) (p * 0 + (1 - p) * 255);
                int green = (int) (p * 255 + (1 - p) * 0);
                g.setColor(new Color(red, green, 0));*/
                g.setColor(p > 0.66 ? Color.GREEN : p > 0.33 ? Color.ORANGE : Color.RED);
                g.fillRect(ax, ay + 1, (int) (p * 2 * aw), 2);

                g.setColor(Color.BLACK);
                g.drawRect(ax, ay, 2 * aw, 3);

                //radius = 4;
                //g.setColor(Color.GREEN);
                //g.fillRect(ox + agent.destination.getX() - radius, oy + agent.destination.getY() - radius, 2 * radius, 2 * radius);
            }
            // ...
        }

        for (SimulationObject object : objects) {
            if (object instanceof Parcel) {
                Parcel parcel = (Parcel) object;
                Point position = parcel.getPosition();
                int x = ox + position.getX();
                int y = oy + position.getY();

                int interval = parcel.getDeadline() - parcel.getArrivalTime();
                double distance = parcel.getDeadline() - this.passedTime;
                distance = Math.max(0, Math.min(distance / interval, 1.0));
                int r = (int) (Math.pow(distance, 1) * 255);
                Color col = new Color(r, (int) (r / 2.0), 0);

                if (Main.DEBUG) {
                    if (parcel.isReserved()) {
                        col = new Color(0, 255, 255);
                    }
                }

                int radius = 3;
                g.setColor(col);//Color.ORANGE);
                g.fillRect(x - radius, y - radius, 2 * radius, 2 * radius);
                g.setColor(Color.BLACK);
                g.drawRect(x - radius, y - radius, 2 * radius, 2 * radius);
            }
            // ...
        }
    }
}