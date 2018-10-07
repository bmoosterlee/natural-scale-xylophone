package gui.buckets;

import java.util.LinkedList;

public class SimpleBucketHistory implements BucketHistory {
    private final LinkedList<Buckets> history;
    private final int size;
    private final double multiplier;

    public SimpleBucketHistory(int size) {
        history = new LinkedList<>();
        this.size = size;
        multiplier = 1. / size;
    }

    private SimpleBucketHistory(int size, LinkedList<Buckets> history, double multiplier) {
        this.size = size;
        this.history = history;
        this.multiplier = multiplier;
    }

    @Override
    public BucketHistory addNewBuckets(Buckets newBuckets) {
        LinkedList<Buckets> newHistory = new LinkedList<>(history);

        if (history.size() >= size) {
            newHistory.removeFirst();
        }
        newHistory.addLast(newBuckets.multiply(multiplier));

        return new SimpleBucketHistory(size, newHistory, multiplier);
    }

    @Override
    public Buckets getTimeAveragedBuckets() {
        Buckets timeAveragedBuckets = new Buckets();
        if (history.isEmpty()) {
            return timeAveragedBuckets;
        }

        for (Buckets buckets : history) {
            timeAveragedBuckets = timeAveragedBuckets.add(buckets);
        }

        return timeAveragedBuckets;
    }

}