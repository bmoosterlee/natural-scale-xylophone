package gui;

public class SpectrumSnapshot {
    final long sampleCount;
    public final Buckets noteBuckets;
    public final Buckets harmonicsBuckets;

    public SpectrumSnapshot(long sampleCount, Buckets noteBuckets, Buckets harmonicsBuckets) {
        this.sampleCount = sampleCount;
        this.noteBuckets = noteBuckets;
        this.harmonicsBuckets = harmonicsBuckets;
    }
}