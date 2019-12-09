package main;

import java.util.*;

public class Graph {

    private int columns;
    private int rows;
    private int cellSize;
    private boolean[] grid;

    private double PENALTY = Main.cellSize;

    public Graph(int rows, int columns, int cellSize) {
        this.rows = rows;
        this.columns = columns;
        this.cellSize = cellSize;
        this.grid = new boolean[rows * columns];
        Arrays.fill(this.grid, true);
    }

    public int getWidth() {
        return this.columns;
    }

    public int getHeight() {
        return this.rows;
    }

    public int getCellSize() {
        return this.cellSize;
    }

    public void setNodeAt(int row, int column, boolean present) {
        this.grid[row + column * this.rows] = present;
    }

    public boolean hasNodeAt(int row, int column) {
        return this.grid[row + column * this.rows];
    }

    public Set<Point> getPoints() {
        Set<Point> points = new HashSet<>();
        int cellSize = this.getCellSize();

        for (int col = 0; col < this.columns; col++) {
            for (int row = 0; row < this.rows; row++) {
                if (!this.hasNodeAt(row, col)) continue;
                Point node = new Point(col * cellSize, row * cellSize);
                points.add(node);
            }
        }

        return points;
    }

    public boolean containsPoint(Point point) {
        int x = point.getX();
        int y = point.getY();
        if (x < 0 || this.columns * this.cellSize <= x || y < 0 || this.rows * this.cellSize <= y) {
            return false;
        }
        if (x % this.cellSize != 0 || y % this.cellSize != 0) {
            return false;
        }
        int col = x / this.cellSize;
        int row = y / this.cellSize;
        return this.hasNodeAt(row, col);
    }

    public Set<Point> getNeighbours(Point point) {
        if (!this.containsPoint(point)) {
            throw new IllegalArgumentException("The graph does not contain the given point");
        }

        Set<Point> neighbours = new HashSet<>();
        Point n1 = new Point(point.getX() + this.cellSize, point.getY());
        Point n2 = new Point(point.getX(), point.getY() + this.cellSize);
        Point n3 = new Point(point.getX() - this.cellSize, point.getY());
        Point n4 = new Point(point.getX(), point.getY() - this.cellSize);
        if (this.containsPoint(n1)) neighbours.add(n1);
        if (this.containsPoint(n2)) neighbours.add(n2);
        if (this.containsPoint(n3)) neighbours.add(n3);
        if (this.containsPoint(n4)) neighbours.add(n4);
        return neighbours;
    }

    public Point getRandomPoint() {
        int row, col;
        do {
            row = (int) Math.floor(Math.random() * this.rows);
            col = (int) Math.floor(Math.random() * this.columns);
        } while (!this.hasNodeAt(row, col));
        return new Point(col * this.cellSize, row * this.cellSize);
    }

    public Point getRandomPointSeeded(Random r) {
        int row, col;
        do {
            row = r.nextInt(this.rows);
            col = r.nextInt(this.columns);
        } while (!this.hasNodeAt(row, col));
        return new Point(col * this.cellSize, row * this.cellSize);
    }

    public List<List<Point>> getMultiplePaths(Point from, Point to, int amount) {
        List<List<Point>> listList = new ArrayList<>();
        HashMap<Point, Double> penalties = new HashMap<>();
        int numFails = 0;
        int maxFails = 3;
        double alpha = Math.random();

        while (listList.size() < amount && numFails < maxFails) {
            List<Point> p = getShortestPathWithPenalties(from, to, penalties);

            if (listList.size() > 1) {
                boolean existsAlready = false;
                for (List<Point> list : listList) {
                    if (Point.pathsEqual(list, p)) {
                        existsAlready = true;
                        break;
                    }
                }
                if (existsAlready) {
                    numFails += 1;
                }
                else {
                    listList.add(p);
                    numFails = 0;
                }
            }
            else {
                listList.add(p);
            }

            for (Point point : p) {
                double beta = Math.random();
                if (beta < alpha) {
                    penalties.put(point, this.PENALTY);
                }
            }
        }

        return listList;
    }

    public List<Point> getShortestPathWithPenalties(Point from, Point to, HashMap<Point, Double> penalties) {
        if (!this.containsPoint(from) || !this.containsPoint(to)) {
            throw new IllegalArgumentException("The graph does not contain both the given nodes");
        }

        // A* Algorithm

        class Vertex {
            private Point point;
            private double distance;
            private Vertex previous;

            private Vertex(Point point, double distance, Vertex previous) {
                this.point = point;
                this.distance = distance;
                this.previous = previous;
            }
        }

        Comparator<Vertex> comparator = (Vertex v1, Vertex v2)
                -> (int) ((v1.distance + Point.distance(v1.point, to)) - (v2.distance + Point.distance(v2.point, to)));
        PriorityQueue<Vertex> vertices = new PriorityQueue<>(32, comparator);
        HashMap<Point, Vertex> mapping = new HashMap<>();
        HashSet<Point> evaluated = new HashSet<>();
        vertices.add(new Vertex(from, 0, null));
        boolean found = false;
        Vertex best = null;

        while (vertices.size() > 0) {
            best = vertices.poll();
            Point point = best.point;
            evaluated.add(point);

            if (point.equals(to)) {
                found = true;
                break;
            }

            Set<Point> neighbours = this.getNeighbours(point);
            for (Point neighbour : neighbours) {
                if (evaluated.contains(neighbour)) continue;
                if (Main.simulation.getNodeAt(neighbour).isBlocked()) continue;

                double distance = best.distance + Point.distance(point, neighbour);

                // penalty added
                if (penalties.keySet().contains(neighbour)) {
                    distance += penalties.get(neighbour);
                }

                Vertex newVertex = new Vertex(neighbour, distance, best);

                if (mapping.containsKey(neighbour)) {
                    Vertex oldVertex = mapping.get(neighbour);
                    if (distance < oldVertex.distance) {
                        vertices.remove(oldVertex);
                        vertices.add(newVertex);
                        mapping.put(neighbour, newVertex);
                    }
                } else {
                    vertices.add(newVertex);
                    mapping.put(neighbour, newVertex);
                }
            }
        }

        if (!found) {
            throw new IllegalArgumentException("There is no path between the given nodes in the graph");
        }

        ArrayList<Point> points = new ArrayList<>();
        Vertex vertex = best;
        while (vertex != null) {
            points.add(vertex.point);
            vertex = vertex.previous;
        }
        Collections.reverse(points);
        return points;
    }

    public List<Point> getShortestPath(Point from, Point to) {
        if (!this.containsPoint(from) || !this.containsPoint(to)) {
            throw new IllegalArgumentException("The graph does not contain both the given nodes");
        }

        // A* Algorithm

        class Vertex {
            private Point point;
            private double distance;
            private Vertex previous;

            private Vertex(Point point, double distance, Vertex previous) {
                this.point = point;
                this.distance = distance;
                this.previous = previous;
            }
        }

        Comparator<Vertex> comparator = (Vertex v1, Vertex v2)
                -> (int) ((v1.distance + Point.distance(v1.point, to)) - (v2.distance + Point.distance(v2.point, to)));
        PriorityQueue<Vertex> vertices = new PriorityQueue<>(32, comparator);
        HashMap<Point, Vertex> mapping = new HashMap<>();
        HashSet<Point> evaluated = new HashSet<>();
        vertices.add(new Vertex(from, 0, null));
        boolean found = false;
        Vertex best = null;

        while (vertices.size() > 0) {
            best = vertices.poll();
            Point point = best.point;
            evaluated.add(point);

            if (point.equals(to)) {
                found = true;
                break;
            }

            Set<Point> neighbours = this.getNeighbours(point);
            for (Point neighbour : neighbours) {
                if (evaluated.contains(neighbour)) continue;
                if (Main.simulation.getNodeAt(neighbour).isBlocked()) continue;

                double distance = best.distance + Point.distance(point, neighbour);
                Vertex newVertex = new Vertex(neighbour, distance, best);

                if (mapping.containsKey(neighbour)) {
                    Vertex oldVertex = mapping.get(neighbour);
                    if (distance < oldVertex.distance) {
                        vertices.remove(oldVertex);
                        vertices.add(newVertex);
                        mapping.put(neighbour, newVertex);
                    }
                } else {
                    vertices.add(newVertex);
                    mapping.put(neighbour, newVertex);
                }
            }
        }

        if (!found) {
            throw new IllegalArgumentException("There is no path between the given nodes in the graph");
        }

        ArrayList<Point> points = new ArrayList<>();
        Vertex vertex = best;
        while (vertex != null) {
            points.add(vertex.point);
            vertex = vertex.previous;
        }
        Collections.reverse(points);
        return points;
    }

    public Graph copy() {
        Graph graph = new Graph(this.rows, this.columns, this.cellSize);

        for (int col = 0; col < this.columns; col++) {
            for (int row = 0; row < this.rows; row++) {
                graph.setNodeAt(row, col, this.hasNodeAt(row, col));
            }
        }

        return graph;
    }
}