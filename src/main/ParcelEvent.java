package main;

public class ParcelEvent {

    public Point from;
    public Point to;
    public int arrivalTime;

    public ParcelEvent(Point from, Point to, int arrivalTime) {
        this.from = from;
        this.to = to;
        this.arrivalTime = arrivalTime;
    }
}