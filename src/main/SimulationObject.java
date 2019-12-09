package main;

public abstract class SimulationObject {

    protected Point position;

    public SimulationObject(Point position) {
        this.position = position;
    }

    public SimulationObject(int x, int y) {
        this(new Point(x, y));
    }

    public void setPosition(Point position) {
        this.position = position;
    }

    public Point getPosition() {
        return this.position;
    }

    public abstract void init(Simulation simulation, Graph graph);
    public abstract void tick();

    @Override
    public String toString(){
        return this.position.toString();
    }
}