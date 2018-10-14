package gui.spectrum.state;

import gui.buckets.BucketHistory;
import gui.buckets.Buckets;

class SpectrumState {
    final Buckets noteBuckets;
    final Buckets harmonicsBuckets;
    final BucketHistory bucketHistory;

    SpectrumState(Buckets noteBuckets, Buckets harmonicsBuckets, BucketHistory bucketHistory) {
        this.noteBuckets = noteBuckets;
        this.harmonicsBuckets = harmonicsBuckets;
        this.bucketHistory = bucketHistory;
    }
}