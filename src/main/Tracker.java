package main;

public class Tracker {

    public static int totalTime = 0;
    public static int count = 0;

    public static void init() {
        totalTime = 0;
        count = 0;
    }

    public static void addParcelDelivery(Parcel parcel, int time) {
        totalTime += time - parcel.getArrivalTime();
        count += 1;
    }

    public static int getAverageParcelTime() {
        return totalTime / count;
    }
}
