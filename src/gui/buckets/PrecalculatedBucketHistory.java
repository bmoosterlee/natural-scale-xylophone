package gui.buckets;

import java.util.LinkedList;

public class PrecalculatedBucketHistory implements BucketHistory {
    LinkedList<Buckets> harmonicsBucketsHistory;
    private int size;
    private double multiplier;
    Buckets timeAverage;

    public PrecalculatedBucketHistory(int size) {
        harmonicsBucketsHistory = new LinkedList<>();
        this.size = size;

        timeAverage = new Buckets();

        multiplier = 1. / size;
    }

    @Override
    public void addNewBuckets(Buckets newBuckets) {
        synchronized (harmonicsBucketsHistory) {
            if (harmonicsBucketsHistory.size() >= size) {
                Buckets removed = harmonicsBucketsHistory.pollFirst();
                timeAverage = timeAverage.subtract(removed);
            }

            Buckets added = newBuckets.multiply(multiplier);

            harmonicsBucketsHistory.addLast(added);
            timeAverage = timeAverage.add(added);
        }
    }

    @Override
    public Buckets getTimeAveragedBuckets() {
        synchronized (harmonicsBucketsHistory) {
            if (harmonicsBucketsHistory.isEmpty()) {
                return new Buckets();
            }
            else {
                return timeAverage;
            }
        }
    }

}