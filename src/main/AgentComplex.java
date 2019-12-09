package main;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * An AGV agent with a "complex" battery strategy.
 * It will take into account its battery status when planning routes.
 */
public class AgentComplex extends Agent {

    public AgentComplex(int x, int y) {
        super(x, y);
    }

    @Override
    public int getBestParcelPath(List<Point> path, Parcel parcel) {
        int time1 = this.getShortestUnreservedPath(path, this.position, parcel.getPosition());
        if (time1 < 0) return -1;
        path.remove(path.size() - 1);
        int time2 = this.getShortestUnreservedPath(path, parcel.getPosition(), parcel.getDeliveryLocation());
        if (time2 < 0) return -1;

        int shortestDistance = this.getDistanceToClosestBatteryStation(parcel.getDeliveryLocation());
        if (shortestDistance < 0) {
            return -1;
        }

        int batteryUsageToParcel = time1 / NODE_TRAVEL_TIME;
        int batteryUsageToDelivery = time2 / NODE_TRAVEL_TIME;
        int batteryUsageToCharger = shortestDistance - 1;
        if (this.batteryLevel - 10 < batteryUsageToParcel + batteryUsageToDelivery + batteryUsageToCharger) {
            return -2;
        }

        return this.simulation.getPassedTime() + time1 + time2;
    }
}