package gui;

import gui.buckets.BucketHistory;
import gui.buckets.Buckets;

public class SpectrumState {
    public final Buckets noteBuckets;
    public final Buckets harmonicsBuckets;
    public final BucketHistory bucketHistory;

    public SpectrumState(Buckets noteBuckets, Buckets harmonicsBuckets, BucketHistory bucketHistory) {
        this.noteBuckets = noteBuckets;
        this.harmonicsBuckets = harmonicsBuckets;
        this.bucketHistory = bucketHistory;
    }
}