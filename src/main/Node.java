package main;

import java.util.ArrayList;
import java.util.List;

public class Node extends SimulationObject {

    private Simulation simulation;
    private Graph graph;

    private List<Reservation> reservations = new ArrayList<>();
    private boolean blocked = false;
    private int intervalTimer = 0;

    public Node(int x, int y) {
        super(x, y);
    }

    @Override
    public void init(Simulation simulation, Graph graph) {
        this.simulation = simulation;
        this.graph = graph;
    }

    @Override
    public void tick() {
        this.intervalTimer += 1;

        if (this.intervalTimer > 60) {
            this.intervalTimer = 0;

            List<Reservation> newReservations = new ArrayList<>();
            for (Reservation r : this.reservations) {
                if (r.getEndTime() >= this.simulation.getPassedTime()) {
                    newReservations.add(r);
                }
            }
            this.reservations = newReservations;
        }
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public boolean isBlocked() {
        return this.blocked;
    }

    public boolean isReserved(int start, int end) {
        for (Reservation reservation : this.reservations) {
            if (start < reservation.getEndTime() && reservation.getStartTime() < end) {
                return true;
            }
        }
        return false;
    }

    public void reserve(Agent agent, int start, int end) {
        this.reservations.add(new Reservation(start, end, agent));
    }

    public void cancelReservationsFor(Agent agent) {
        List<Reservation> newReservations = new ArrayList<>();
        for (Reservation r : this.reservations) {
            if (r.getAgent() != agent) {
                newReservations.add(r);
            }
        }
        this.reservations = newReservations;
    }

    @Override
    public String toString(){
        return this.getPosition().toString();
    }

}
