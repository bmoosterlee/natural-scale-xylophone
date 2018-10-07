package gui.spectrum.state;

import gui.buckets.Buckets;
import gui.buckets.PrecalculatedBucketHistory;

public class SpectrumManager {
    private SpectrumState spectrumState;

    public SpectrumManager() {
        setSpectrumState(new SpectrumState(new Buckets(), new Buckets(), new PrecalculatedBucketHistory(300)));
    }

    public SpectrumState getSpectrumState() {
        return spectrumState;
    }

    public void setSpectrumState(SpectrumState spectrumState) {
        this.spectrumState = spectrumState;
    }
}