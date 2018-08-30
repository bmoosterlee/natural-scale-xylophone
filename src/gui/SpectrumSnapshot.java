package gui;

public class SpectrumSnapshot {
    public final Buckets noteBuckets;
    public final Buckets harmonicsBuckets;

    public SpectrumSnapshot(Buckets noteBuckets, Buckets harmonicsBuckets) {
        this.noteBuckets = noteBuckets;
        this.harmonicsBuckets = harmonicsBuckets;
    }

    public Buckets getNoteBuckets() {
        return noteBuckets;
    }
}