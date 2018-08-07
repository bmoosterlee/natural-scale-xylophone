import java.util.LinkedList;

public class BucketHistory {
    LinkedList<Buckets> harmonicsBucketsHistory;
    private int size;
    private double multiplier;
    private Buckets buckets;

    public BucketHistory(int size) {
        harmonicsBucketsHistory = new LinkedList<>();
        this.size = size;
        multiplier = 1. / size;
    }

    Buckets getNewBuckets(Buckets newBuckets) {
        if (harmonicsBucketsHistory.size() >= size) {
            synchronized (harmonicsBucketsHistory) {
                harmonicsBucketsHistory.removeFirst();
            }
        }
        Buckets multiply = newBuckets.multiply(multiplier);
        synchronized (harmonicsBucketsHistory) {
            harmonicsBucketsHistory.addLast(multiply);
        }
        return getTimeAveragedBuckets(newBuckets.getLength());
    }

    Buckets getTimeAveragedBuckets(int length) {
        Buckets timeAveragedBuckets = new Buckets(length);
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