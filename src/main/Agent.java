package main;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public abstract class Agent extends SimulationObject {

    Simulation simulation;
    Graph graph;
    Point startPosition;
    int direction = 0;

    final int NODE_TRAVEL_TIME = Main.cellSize;


    boolean inBetweenNodes = false;
    boolean transporting = false;
    Parcel currentParcel = null;
    List<Point> currentPath = null;
    Point nextPosition = null;
    Point currentDestination = null;

    int batteryLevelFull = Main.batteryCapacity;
    int batteryLevel = this.batteryLevelFull;
    boolean wantsToCharge = false;
    BatteryStation currentBatteryStation = null;

    class ParcelPath {
        private Parcel parcel;
        private List<Point> path;
        private int deliveryTime;
        private ParcelPath(Parcel parcel, List<Point> path, int deliveryTime) {
            this.parcel = parcel;
            this.path = path;
            this.deliveryTime = deliveryTime;
        }
    }

    public Agent(int x, int y) {
        super(x, y);
        this.startPosition = new Point(x, y);
    }

    public Point getStartPosition() {
        return this.startPosition;
    }

    @Override
    public void init(Simulation simulation, Graph graph) {
        this.simulation = simulation;
        this.graph = graph;

        this.simulation.getNodeAt(this.position).setBlocked(true);
    }

    private static int delayTime = 10;
    private int delay = 0;

    @Override
    public void tick() {
        if (this.inBetweenNodes) {
            this.delay = 0;
            this.move();
        } else if (batteryLevel > 0) {
            if (this.delay > 0) {
                this.delay -= 1;
            } else {
                this.delay = delayTime;

                if (this.wantsToCharge) {
                    this.goChargeBattery();
                } else if (this.currentParcel != null) {
                    if (this.transporting) {
                        this.deliverParcel();
                    } else {
                        this.searchParcel();
                        this.pickUpParcel();
                    }
                } else {
                    this.searchParcel();
                }

                this.updateBatteryManagement();
                this.updatePathMovement();
            }
        }
    }

    public void updatePathMovement() {
        this.getOutOfTheWay();

        if (this.currentPath == null) {
            if (this.transporting) {
                reroutePath();
            }
            return;
        }

        this.currentPath = this.currentPath.subList(1, this.currentPath.size());

        if (this.currentPath.size() == 0) {
            this.cancelParcelTask();
            this.currentPath = null;
            return;
        }

        this.nextPosition = this.currentPath.get(0);
        Node nextNode = this.simulation.getNodeAt(nextPosition);

        if (nextNode.isBlocked()) {
            reroutePath();
        } else {
            Node currentNode = this.simulation.getNodeAt(this.position);
            currentNode.setBlocked(false);
            nextNode.setBlocked(true);
            this.inBetweenNodes = true;
        }
    }

    public void reroutePath() {
        if (this.transporting) {
            this.cancelCurrentPath();
            List<Point> path = new ArrayList<>();
            int time = this.getShortestUnreservedPath(path, this.position, this.currentParcel.getDeliveryLocation());
            if (time >= 0) {
                this.selectAndReservePath(path);
            }
        } else {
            this.cancelParcelTask();
            if (currentBatteryStation != null) {
                this.currentBatteryStation.free();
                this.currentBatteryStation = null;
            }
        }
    }

    public void cancelCurrentPath() {
        if (this.currentPath == null) return;

        for (Point p : this.currentPath) {
            Node node = this.simulation.getNodeAt(p);
            node.cancelReservationsFor(this);
        }

        this.currentPath = null;
    }

    public void cancelParcelTask() {
        if (this.currentParcel != null) {
            this.currentParcel.setBeingTransported(false);
            this.currentParcel.free();
            this.currentParcel = null;
        }

        this.transporting = false;
        this.cancelCurrentPath();
    }

    public void updateBatteryManagement() {
        /*if (!this.wantsToCharge) {
            // check if it is necessary to go charge battery nonetheless, just to be sure not to run out of energy
            int distance = this.getDistanceToClosestBatteryStation(this.position);
            if (this.batteryLevel < distance + 10) {
                if (this.currentParcel != null) {
                    if (this.transporting) {
                        this.transporting = false;
                        this.currentParcel.setBeingTransported(false);
                    }
                    this.cancelParcelTask();
                }
                this.wantsToCharge = true;
            }
        }*/

        if (this.wantsToCharge && (this.currentBatteryStation == null ||
                    (this.currentPath == null && !this.getPosition().equals(this.currentBatteryStation.getPosition())))) {
            int distance = Integer.MAX_VALUE;
            BatteryStation bestStation = null;

            for (BatteryStation station : this.simulation.getBatteryStations()) {
                if (station.isReserved()) continue;
                List<Point> path;
                try {
                    path = this.graph.getShortestPath(this.position, station.getPosition());
                } catch (IllegalArgumentException e) {
                    continue;
                }
                if (path.size() > this.batteryLevel) continue;  // can't reach that
                if (path.size() < distance) {
                    distance = path.size();
                    bestStation = station;
                }
            }

            if (bestStation != null) {
                List<Point> path = new ArrayList<>();
                int time = this.getShortestUnreservedPath(path, this.position, bestStation.getPosition());
                if (time >= 0) {
                    this.selectAndReservePath(path);
                    this.currentBatteryStation = bestStation;
                    this.currentBatteryStation.reserve(this);
                }
            }
        }
    }

    public int getDistanceToClosestBatteryStation(Point from) {
        int shortestDistance = Integer.MAX_VALUE;

        for (BatteryStation station : this.simulation.getBatteryStations()) {
            List<Point> p;
            try {
                p = this.graph.getShortestPath(from, station.getPosition());
            } catch (IllegalArgumentException e) {
                continue;
            }
            if (p.size() < shortestDistance) {
                shortestDistance = p.size();
            }
        }

        if (shortestDistance == Integer.MAX_VALUE) {
            return -1;
        }

        return shortestDistance;
    }

    public void goChargeBattery() {
        if (this.currentBatteryStation != null && this.position.equals(this.currentBatteryStation.getPosition())) {
            if (this.batteryLevel == this.batteryLevelFull) {
                this.wantsToCharge = false;
                this.currentBatteryStation = null;
                Main.timesCharged += 1;
            }
        }
    }

    public void deliverParcel() {
        if (this.position.equals(this.currentParcel.getDeliveryLocation())) {
            this.simulation.parcelDelivered(this.currentParcel);

            this.currentParcel.setBeingTransported(false);
            this.simulation.unregister(this.currentParcel);
            this.transporting = false;
            this.cancelParcelTask();

            //this.searchParcel();
        }
    }

    public void pickUpParcel() {
        if (this.currentParcel == null) return;

        if (this.position.equals(this.currentParcel.getPosition())) {
            this.currentParcel.setBeingTransported(true);
            this.transporting = true;
        }
    }

    public void searchParcel() {
        //int other = 0;
        int batteryTooLow = 0;
        PriorityQueue<ParcelPath> queue = new PriorityQueue<>(16, this::heuristic);

        for (Parcel parcel : this.simulation.getParcels()) {
            List<Point> bestPath = new ArrayList<>();
            int deliveryTime = this.getBestParcelPath(bestPath, parcel);
            if (deliveryTime == -2) batteryTooLow += 1;
            //if (deliveryTime == -1) other += 1;
            if (deliveryTime < 0) continue;

            queue.add(new ParcelPath(parcel, bestPath, deliveryTime));
        }

        ParcelPath bestParcelPath = null;

        while (queue.size() > 0) {
            ParcelPath parcelPath = queue.poll();
            Parcel parcel = parcelPath.parcel;
            ParcelPath parcelParcelPath = new ParcelPath(parcel, null, parcel.getCurrentDeliveryTime());

            if (!parcel.isReserved() || this.heuristic(parcelPath, parcelParcelPath) < 0) {
                bestParcelPath = parcelPath;
                break;
            }
        }

        ParcelPath currentParcelPath = null;
        if (this.currentParcel != null) {
            currentParcelPath = new ParcelPath(this.currentParcel, this.currentPath, this.currentParcel.getCurrentDeliveryTime());
        }

        if (bestParcelPath != null) {
            if (this.currentParcel == null || this.heuristic(bestParcelPath,currentParcelPath) < 0) {
                this.cancelParcelTask();
                this.currentParcel = bestParcelPath.parcel;
                bestParcelPath.parcel.reserve(this, bestParcelPath.deliveryTime);
                this.selectAndReservePath(bestParcelPath.path);
            }
        } else if (/*this.checkNeedToCharge && */batteryTooLow > 0) {
            this.cancelParcelTask();
            this.wantsToCharge = true;
            //System.out.println("Battery too low and no parcel task found that's doable.");
            //this.checkNeedToCharge = false;
        }
    }

    public int heuristic(ParcelPath o1, ParcelPath o2) {
        if (o1 == o2) return 0;

        if (o1.deliveryTime < o1.parcel.getDeadline() && o2.deliveryTime > o2.parcel.getDeadline()) {
            return 1;
        } else if (o2.deliveryTime < o2.parcel.getDeadline() && o1.deliveryTime > o1.parcel.getDeadline()) {
            return -1;
        } else if (o1.deliveryTime > o1.parcel.getDeadline() && o2.deliveryTime > o2.parcel.getDeadline()) {
            if (o1.parcel.getDeadline() > o2.parcel.getDeadline()) {
                return 1;
            } else if (o1.parcel.getDeadline() < o2.parcel.getDeadline()) {
                return -1;
            } else {
                if (o1.deliveryTime < o2.deliveryTime) {
                    return -1;
                } else {
                    return 1;
                }
            }
        } else {
            if (o1.deliveryTime < o2.deliveryTime) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    public int getBestParcelPath(List<Point> path, Parcel parcel) {
        int time1 = this.getShortestUnreservedPath(path, this.position, parcel.getPosition());
        if (time1 < 0) return -1;
        path.remove(path.size() - 1);
        int time2 = this.getShortestUnreservedPath(path, parcel.getPosition(), parcel.getDeliveryLocation());
        if (time2 < 0) return -1;

        return this.simulation.getPassedTime() + time1 + time2;
    }

    public int getShortestUnreservedPath(List<Point> path, Point from, Point to) {
        List<List<Point>> paths;

        try {
            paths = this.graph.getMultiplePaths(from, to, 5);
        } catch (IllegalArgumentException e) {
            return -1;
        }

        int currentTime = this.simulation.getPassedTime();
        int bestTime = Integer.MAX_VALUE;
        List<Point> bestPath = null;

        for (List<Point> p : paths) {
            int totalPathTime = this.sendExplorationAnt(p, currentTime);
            if (totalPathTime < 0) continue;
            if (totalPathTime < bestTime) {
                bestTime = totalPathTime;
                bestPath = p;
            }
        }

        if (bestPath == null) {
            return -1;
        }

        path.addAll(bestPath);
        return bestTime;
    }

    public int sendExplorationAnt(List<Point> path, int currentTime) {
        int time = currentTime;

        for (int i = 1; i < path.size(); i++) {
            Node node = this.simulation.getNodeAt(path.get(i));
            if (node.isReserved(time, time + NODE_TRAVEL_TIME)) {
                return -1;
            }
            time += NODE_TRAVEL_TIME;
        }

        return time - currentTime;
    }

    public void selectAndReservePath(List<Point> path) {
        if (this.currentPath != null) {
            this.cancelCurrentPath();
        }
        this.currentPath = path;
        this.currentDestination = path.get(path.size() - 1);
        sendReservationAnt(path, this.simulation.getPassedTime());
    }

    public void sendReservationAnt(List<Point> path, int currentTime) {
        int time = currentTime;

        for (int i = 1; i < path.size(); i++) {
            Node node = this.simulation.getNodeAt(path.get(i));
            node.reserve(this, time, time + NODE_TRAVEL_TIME);
            time += NODE_TRAVEL_TIME;
        }
    }

    private int blockedParameter = 0;
    private Point previousLocation = this.getPosition();

    public void getOutOfTheWay() {
        if (this.previousLocation.equals(this.getPosition())) {
            this.blockedParameter += 1;
        } else {
            this.blockedParameter = 0;
            this.previousLocation = this.getPosition();
        }
        if (blockedParameter >= 1000) {
            this.goToRandomFreeLocation();
        }

        if (this.position.equals(this.currentDestination) ) {
            boolean done = false;

            if (!this.wantsToCharge) {
                for (BatteryStation station : this.simulation.getBatteryStations()) {
                    if (station.getPosition().equals(this.position)) {
                        this.goToRandomFreeLocation();
                        done = true;
                        break;
                    }
                }
            }

            if (!done) {
                for (DeliveryLocation delivery : this.simulation.getDeliveryLocations()) {
                    if (delivery.getPosition().equals(this.position)) {
                        this.goToRandomFreeLocation();
                        break;
                    }
                }
            }
        }
    }

    public void goToRandomFreeLocation() {
        this.cancelParcelTask();
        this.transporting = false;
        this.currentBatteryStation = null;

        List<Point> path = new ArrayList<>();
        Point freePoint = this.findFreeLocation();
        int time = this.getShortestUnreservedPath(path, this.position, freePoint);
        if (time >= 0) {
            if (path.size() > 4) {
                path = path.subList(0, 4);
            }
            this.selectAndReservePath(path);
        }
    }

    public Point findFreeLocation() {
        while (true) {
            Point point = this.graph.getRandomPoint();
            boolean free = true;
            for (SimulationObject object : this.simulation.getObjects()) {
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

    public void move() {
        int speed = 1;
        Point point1 = this.position;
        Point point2 = this.nextPosition;
        //Point point2 = this.currentPath.get(0);
        int dx = point2.getX() - point1.getX();
        int dy = point2.getY() - point1.getY();
        int speedX = Math.min(Math.abs(dx), speed);
        int speedY = Math.min(Math.abs(dy), speed);
        dx = (int) Math.signum((double) dx) * speedX;
        dy = (int) Math.signum((double) dy) * speedY;
        this.position = Point.add(this.position, new Point(dx, dy));

        if (this.position.equals(point2)) {
            this.inBetweenNodes = false;

            this.batteryLevel -= 1;
            if (this.batteryLevel <= 0) {
                this.batteryLevel = 0;
                System.out.println("*BATTERY DEPLETED*");
            }
            if (this.batteryLevel < 15) {
                //System.out.println("OUT OF ENERGY: " + this.batteryLevel);
            }
        }

        if (this.transporting) {
            this.currentParcel.setPosition(this.position);
        }

        // for drawing
        if (dx == 0) {
            this.direction = dy < 0 ? 90 : 270;
        } else if (dy == 0) {
            this.direction = dx < 0 ? 180 : 0;
        }
    }
}
