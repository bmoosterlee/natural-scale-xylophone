package gui;

import harmonics.HarmonicCalculator;
import notes.*;

import java.util.*;

public class SpectrumWindow {
    public NoteManager noteManager;
    public HarmonicCalculator harmonicCalculator;


    private final BucketHistory bucketHistory = new BucketHistory(300);
    public final Frequency centerFrequency = new Frequency(2 * 261.63);
    double octaveRange = 3.;
    public Frequency lowerBound;
    public Frequency upperBound;
    double logFrequencyMultiplier;
    double logFrequencyAdditive;
    double xMultiplier;

    public SpectrumWindow(NoteManager noteManager, HarmonicCalculator harmonicCalculator) {
        this.noteManager = noteManager;
        this.harmonicCalculator = harmonicCalculator;

        lowerBound = centerFrequency.divideBy(Math.pow(2, octaveRange / 2));
        upperBound = centerFrequency.multiplyBy(Math.pow(2, octaveRange / 2));

        double logLowerBound = Math.log(lowerBound.getValue());
        double logUpperBound = Math.log(upperBound.getValue());
        double logRange = logUpperBound - logLowerBound;
        logFrequencyMultiplier = gui.GUI.WIDTH / logRange;
        logFrequencyAdditive = logLowerBound * gui.GUI.WIDTH / logRange;
        xMultiplier = logRange / gui.GUI.WIDTH;
    }

    SpectrumSnapshotBuilder createBuilder(long sampleCount) {
        return new SpectrumSnapshotBuilder(sampleCount, this);
    }

    Set<Frequency> clip(Set<Frequency> liveFrequencies) {
        Set<Frequency> clippedFrequencies = new HashSet<>();
        for (Frequency frequency : liveFrequencies) {
            int x = getX(frequency);
            if (x < 0 || x >= gui.GUI.WIDTH) {
                continue;
            }
            clippedFrequencies.add(frequency);
        }
        return clippedFrequencies;
    }

    public int getX(Frequency frequency) {
        return (int) (Math.log(frequency.getValue()) * logFrequencyMultiplier - logFrequencyAdditive);
    }

    public Frequency getFrequency(double x) {
        //todo precalculate the multiplier for each x (not xMultiplier), and precalc the frequency for each x
        return lowerBound.multiplyBy(Math.exp(x * xMultiplier));
    }

    public Frequency getCenterFrequency() {
        return centerFrequency;
    }

    public BucketHistory getBucketHistory() {
        return bucketHistory;
    }

}