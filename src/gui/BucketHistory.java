package gui;

public interface BucketHistory {
    void addNewBuckets(Buckets newBuckets);

    Buckets getTimeAveragedBuckets();
}
