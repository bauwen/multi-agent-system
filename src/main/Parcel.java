package main;

import java.util.List;
import java.util.Random;

public class Parcel extends SimulationObject  {

    private Simulation simulation;
    private Graph graph;

    private Point startLocation;
    private int arrivalTime;
    private int deadline;
    private Point deliveryLocation;
    private Agent reservedAgent;
    private int currentDeliveryTime = Integer.MAX_VALUE;
    private boolean beingTransported = false;

    public Parcel(Point from, Point to, int arrivalTime) {
        super(from.getX(), from.getY());
        this.startLocation = from;
        this.deliveryLocation = to;
        this.arrivalTime = arrivalTime;

        deadline = determineDeadline(from, to, arrivalTime);
    }

    public int getCurrentDeliveryTime(){
        return this.currentDeliveryTime;
    }

    private int determineDeadline(Point from, Point to, int arrivalTime) {
        int deltaX = Math.abs(from.getX() - to.getX());
        int deltaY = Math.abs(from.getY() - to.getY());
        int transportTime = deltaX + deltaY;

        //margin determined by the max length
        Random r = new Random();
        int margin = (int) ((Main.gridWidth + Main.gridHeight)*Main.cellSize* (r.nextDouble()*2.0+2.0));

        return arrivalTime + transportTime + margin;
    }

    @Override
    public void init(Simulation simulation, Graph graph) {
        this.simulation = simulation;
        this.graph = graph;

        List<DeliveryLocation> locs = this.simulation.getDeliveryLocations();
        int index = (int) (Math.random() * locs.size());
        DeliveryLocation loc = locs.get(index);
        this.deliveryLocation = loc.getPosition();
    }

    @Override
    public void tick() {
        if(this.reservedAgent == null && this.simulation.getDeliveryLocationAt(this.getPosition())!= null){
            this.simulation.unregister(this);
            this.free();
        }
    }

    public int getDeadline() {
        return this.deadline;
    }

    public int getArrivalTime() {
        return this.arrivalTime;
    }

    public Point getDeliveryLocation() {
        return this.deliveryLocation;
    }

    public boolean isBeingTransported(){
        return beingTransported;
    }

    public void setBeingTransported(boolean beingTransported){
        this.beingTransported = beingTransported;
    }

    public boolean isReserved() {
        return this.reservedAgent != null;
    }

    public void reserve(Agent agent, int deliveryTime) {
        if (this.reservedAgent != null) {
            this.reservedAgent.cancelParcelTask();
        }

        this.reservedAgent = agent;
        this.currentDeliveryTime = deliveryTime;
        //this.reservationScore = heuristic(deliveryTime);
    }

    public void free() {
        this.reservedAgent = null;
        //this.reservationScore = Integer.MIN_VALUE;
        this.currentDeliveryTime = Integer.MAX_VALUE;
    }

    public Point getStartLocation(){
        return this.startLocation;
    }

    /*
    public int getDistance() {
        return Math.abs(deliveryLocation.getX() - startLocation.getX()) + Math.abs(deliveryLocation.getY() - startLocation.getY());
    }
    */
}
