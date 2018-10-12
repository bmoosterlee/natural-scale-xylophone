package gui.spectrum.state;

import frequency.state.FrequencyManager;
import gui.buckets.Buckets;
import gui.buckets.PrecalculatedBucketHistory;
import gui.spectrum.SpectrumWindow;
import harmonics.HarmonicCalculator;
import notes.envelope.EnvelopeManager;
import time.PerformanceTracker;
import time.TimeInNanoSeconds;
import time.TimeKeeper;

public class SpectrumManager {
    public FrequencyManager frequencyManager;
    public HarmonicCalculator harmonicCalculator;
    public EnvelopeManager envelopeManager;

    private SpectrumState spectrumState;

    public SpectrumManager(FrequencyManager frequencyManager, EnvelopeManager envelopeManager, HarmonicCalculator harmonicCalculator) {
        this.frequencyManager = frequencyManager;
        this.envelopeManager = envelopeManager;
        this.harmonicCalculator = harmonicCalculator;

        this.spectrumState = new SpectrumState(new Buckets(), new Buckets(), new PrecalculatedBucketHistory(200));
    }

    public SpectrumState getSpectrumState() {
        return spectrumState;
    }

    public SpectrumState getSpectrumState(SpectrumWindow spectrumWindow, long expectedTickCount, TimeInNanoSeconds frameEndTime) {
        TimeKeeper timeKeeper = PerformanceTracker.startTracking("create spectrum snapshot");
        SpectrumStateBuilder spectrumStateBuilder = new SpectrumStateBuilder(this, spectrumState, expectedTickCount, spectrumWindow);
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("build spectrum snapshot");
        while (TimeInNanoSeconds.now().lessThan(frameEndTime)) {
            if (spectrumStateBuilder.update()) break;
        }
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("finish building spectrum snapshot");
        SpectrumState spectrumState = spectrumStateBuilder.finish();
        this.spectrumState = spectrumState;
        PerformanceTracker.stopTracking(timeKeeper);
        return spectrumState;
    }
}