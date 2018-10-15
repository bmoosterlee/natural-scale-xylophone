package gui.spectrum.state;

import gui.buckets.Buckets;

class SpectrumState {
    final Buckets noteBuckets;
    final Buckets harmonicsBuckets;

    SpectrumState(Buckets noteBuckets, Buckets harmonicsBuckets) {
        this.noteBuckets = noteBuckets;
        this.harmonicsBuckets = harmonicsBuckets;
    }
}