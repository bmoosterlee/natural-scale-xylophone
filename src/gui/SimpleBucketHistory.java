package gui;

import java.util.LinkedList;

public class BucketHistory {
    LinkedList<Buckets> harmonicsBucketsHistory;
    private int size;
    private double multiplier;

    public BucketHistory(int size) {
        harmonicsBucketsHistory = new LinkedList<>();
        this.size = size;
        multiplier = 1. / size;
    }

    public void addNewBuckets(Buckets newBuckets) {
        synchronized (harmonicsBucketsHistory) {
            if (harmonicsBucketsHistory.size() >= size) {
                harmonicsBucketsHistory.removeFirst();
            }
            harmonicsBucketsHistory.addLast(newBuckets.multiply(multiplier));
        }
    }

    public Buckets getTimeAveragedBuckets() {
        Buckets timeAveragedBuckets = new Buckets();
        if (harmonicsBucketsHistory.isEmpty()) {
            return timeAveragedBuckets;
        }
        synchronized (harmonicsBucketsHistory) {
            for (Buckets buckets : harmonicsBucketsHistory) {
                timeAveragedBuckets = timeAveragedBuckets.add(buckets);
            }
        }
        return timeAveragedBuckets;
    }

}