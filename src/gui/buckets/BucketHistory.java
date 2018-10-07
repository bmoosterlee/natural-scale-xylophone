package gui.buckets;

public interface BucketHistory {
    BucketHistory addNewBuckets(Buckets newBuckets);

    Buckets getTimeAveragedBuckets();
}
