package gui;

public class SpectrumState {
    public final Buckets noteBuckets;
    public final Buckets harmonicsBuckets;

    public SpectrumState(Buckets noteBuckets, Buckets harmonicsBuckets) {
        this.noteBuckets = noteBuckets;
        this.harmonicsBuckets = harmonicsBuckets;
    }
}