package main;

import java.util.List;

public class Point {

    private int x;
    private int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Point(Point other) {
        this.x = other.x;
        this.y = other.y;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Point)) {
            return false;
        }
        Point other = (Point) object;
        return this.x == other.x && this.y == other.y;
    }

    @Override
    public int hashCode() {
        //return this.x + this.y * 997;
        return this.x + this.y * Main.gridWidth;
    }

    @Override
    public String toString(){
        return "Point: " + this.getX()/Main.cellSize + ":" + this.getY()/Main.cellSize;
    }

    public static double distance(Point point1, Point point2) {
        int dx = point1.x - point2.x;
        int dy = point1.y - point2.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public static Point add(Point point1, Point point2) {
        return new Point(point1.x + point2.x, point1.y + point2.y);
    }

    public static boolean pathsEqual(List<Point> path1, List<Point> path2){
        if(path1.size() != path2.size()) {
            return false;
        }

        for (int i = 0; i < path1.size(); i++) {
            if (!path1.get(i).equals(path2.get(i))) {
                return false;
            }
        }

        return true;
    }
}