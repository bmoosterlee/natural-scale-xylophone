package spectrum.buckets;

public interface BucketHistory {
    BucketHistory addNewBuckets(Buckets newBuckets);

    Buckets getTimeAveragedBuckets();
}
