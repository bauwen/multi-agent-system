package main;

public class Reservation {

    private int startTime;
    private int endTime;
    private Agent agent;
    private int refreshTimer;

    public Reservation(int startTime, int endTime, Agent agent) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.agent = agent;
    }

    public int getStartTime() {
        return this.startTime;
    }

    public int getEndTime() {
        return this.endTime;
    }

    public Agent getAgent() {
        return this.agent;
    }
}
