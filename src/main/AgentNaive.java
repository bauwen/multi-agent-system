package main;

import java.util.ArrayList;
import java.util.List;

/**
 * An AGV agent with a "naive" battery strategy.
 * It will try to charge its battery as soon as it reaches a certain threshold (33% of its battery's full capacity).
 */
public class AgentNaive extends Agent {

    public AgentNaive(int x, int y) {
        super(x, y);
    }

    @Override
    public void updateBatteryManagement() {

        boolean shouldWait = true;

        int shortestDistance = this.getDistanceToClosestBatteryStation(this.getPosition());
        if (shortestDistance < 0) {
            double batteryPercentage = (double) this.batteryLevel / this.batteryLevelFull;
            if (batteryPercentage < 0.33) {
                shouldWait = this.transporting && batteryPercentage > 0.20;
            }
        } else {
            if (this.batteryLevel - 10 <= shortestDistance) {
                shouldWait = false;
            }
        }

        if (!shouldWait) {
            if (this.currentParcel != null) {
                if (this.transporting) {
                    this.transporting = false;
                    this.currentParcel.setBeingTransported(false);
                }
                this.cancelParcelTask();
            }
            this.wantsToCharge = true;
        }

        /*
        double batteryPercentage = (double) this.batteryLevel / this.batteryLevelFull;
        if (batteryPercentage < 0.33) {
            boolean shouldWait = this.transporting && batteryPercentage > 0.20;

            if (!shouldWait) {
                if (this.currentParcel != null) {
                    if (this.transporting) {
                        this.transporting = false;
                        this.currentParcel.setBeingTransported(false);
                    }
                    this.cancelParcelTask();
                }
                this.wantsToCharge = true;
            }
        }
        */

        super.updateBatteryManagement();
    }
}