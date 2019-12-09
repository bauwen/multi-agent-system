package main;

public class DeliveryLocation extends SimulationObject {

    private Simulation simulation;
    private Graph graph;

    public DeliveryLocation(int x, int y) {
        super(x, y);
    }

    @Override
    public void init(Simulation simulation, Graph graph) {
        this.simulation = simulation;
        this.graph = graph;
    }

    @Override
    public void tick() {
        // does nothing
    }
}
