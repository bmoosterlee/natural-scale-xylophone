package gui.buckets;

import java.util.LinkedList;

public class PrecalculatedBucketHistory implements BucketHistory {
    private final int size;
    private final LinkedList<Buckets> history;

    private final double multiplier;
    private final Buckets timeAverage;

    public PrecalculatedBucketHistory(int size) {
        history = new LinkedList<>();
        this.size = size;

        timeAverage = new Buckets();

        multiplier = 1. / size;
    }

    private PrecalculatedBucketHistory(int size, LinkedList<Buckets> history, double multiplier, Buckets timeAverage) {
        this.size = size;
        this.history = history;
        this.multiplier = multiplier;
        this.timeAverage = timeAverage;
    }

    @Override
    public BucketHistory addNewBuckets(Buckets newBuckets) {
        LinkedList<Buckets> newHistory = new LinkedList<>(history);
        Buckets newTimeAverage = timeAverage;

        if (history.size() >= size) {
            Buckets removed = newHistory.pollFirst();
            newTimeAverage = newTimeAverage.subtract(removed);
        }

        Buckets added = newBuckets.multiply(multiplier);

        newHistory.addLast(added);
        newTimeAverage = newTimeAverage.add(added);

        return new PrecalculatedBucketHistory(size, newHistory, multiplier, newTimeAverage);
    }

    @Override
    public Buckets getTimeAveragedBuckets() {
        if (history.isEmpty()) {
            return new Buckets();
        }
        else {
            return timeAverage;
        }
    }

}