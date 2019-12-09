package main;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * An AGV agent with a "simple" battery strategy.
 * It will try to charge its battery after K parcel deliveries (regardless of current battery status).
 */
public class AgentSimple extends Agent {

    public AgentSimple(int x, int y) {
        super(x, y);
    }

    public int deliverCount = 0;

    @Override
    public void deliverParcel() {
        if (this.position.equals(this.currentParcel.getDeliveryLocation())) {
            this.simulation.parcelDelivered(this.currentParcel);

            this.currentParcel.setBeingTransported(false);
            this.simulation.unregister(this.currentParcel);
            this.transporting = false;
            this.cancelParcelTask();

            //this.searchParcel();
            deliverCount += 1;
            if (deliverCount == Main.simpleDeliveryCount) {
                this.wantsToCharge = true;
                deliverCount = 0;
            }
        }
    }
}