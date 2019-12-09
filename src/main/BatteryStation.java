package main;

public class BatteryStation extends SimulationObject {

    private Simulation simulation;
    private Graph graph;

    private Agent user = null;
    private boolean charging = false;
    private int chargeDelay = 2;
    private int chargeTimer = 0;

    public BatteryStation(int x, int y) {
        super(x, y);
    }

    public boolean isReserved() {
        return this.user != null;
    }

    public void reserve(Agent agent) {
        if (!this.isReserved()) {
            this.user = agent;
        }
    }

    public void free() {
        this.user = null;
    }

    @Override
    public void init(Simulation simulation, Graph graph) {
        this.simulation = simulation;
        this.graph = graph;
    }

    @Override
    public void tick() {
        if (this.user != null) {
            if (this.charging) {
                if (!this.user.getPosition().equals(this.position)) {
                    this.user = null;
                    this.chargeTimer = 0;
                    this.charging = false;
                    return;
                }

                if (this.chargeTimer < this.chargeDelay) {
                    this.chargeTimer += 1;
                } else {
                    this.chargeTimer = 0;

                    if (this.user.batteryLevel < this.user.batteryLevelFull) {
                        if (Main.chargingMultiplier > 0) {
                            this.user.batteryLevel += 1 * Main.chargingMultiplier;
                        } else {
                            this.user.batteryLevel = this.user.batteryLevelFull;
                        }
                    }
                    if (this.user.batteryLevel > this.user.batteryLevelFull) {
                        this.user.batteryLevel = this.user.batteryLevelFull;
                    }
                }
            } else if (this.user.getPosition().equals(this.position)) {
                this.charging = true;
            }
        }
    }
}
