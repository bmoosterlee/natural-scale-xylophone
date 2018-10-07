package gui.buckets;

public interface BucketHistory {
    void addNewBuckets(Buckets newBuckets);

    Buckets getTimeAveragedBuckets();
}
