package gui;

import harmonics.HarmonicCalculator;
import notes.*;

import java.util.*;

public class SpectrumWindow {
    public NoteManager noteManager;
    public HarmonicCalculator harmonicCalculator;


    private final BucketHistory bucketHistory = new BucketHistory(300);
    public final double centerFrequency = 2 * 261.63;
    double octaveRange = 3.;
    public double lowerBound;
    public double upperBound;
    double logFrequencyMultiplier;
    double logFrequencyAdditive;
    double xMultiplier;

    public SpectrumWindow(NoteManager noteManager, HarmonicCalculator harmonicCalculator) {
        this.noteManager = noteManager;
        this.harmonicCalculator = harmonicCalculator;

        lowerBound = centerFrequency / Math.pow(2, octaveRange / 2);
        upperBound = centerFrequency * Math.pow(2, octaveRange / 2);

        double logLowerBound = Math.log(lowerBound);
        double logUpperBound = Math.log(upperBound);
        double logRange = logUpperBound - logLowerBound;
        logFrequencyMultiplier = gui.GUI.WIDTH / logRange;
        logFrequencyAdditive = logLowerBound * gui.GUI.WIDTH / logRange;
        xMultiplier = logRange / gui.GUI.WIDTH;
    }

    SpectrumSnapshotBuilder createBuilder(long sampleCount) {
        return new SpectrumSnapshotBuilder(sampleCount, this);
    }

    Set<Double> clip(Set<Double> liveFrequencies) {
        Set<Double> clippedFrequencies = new HashSet<>();
        for (Double frequency : liveFrequencies) {
            int x = getX(frequency);
            if (x < 0 || x >= gui.GUI.WIDTH) {
                continue;
            }
            clippedFrequencies.add(frequency);
        }
        return clippedFrequencies;
    }

    public int getX(double frequency) {
        return (int) (Math.log(frequency) * logFrequencyMultiplier - logFrequencyAdditive);
    }

    public double getFrequency(double x) {
        return Math.exp(x * xMultiplier) * lowerBound;
    }

    public double getCenterFrequency() {
        return centerFrequency;
    }

    public BucketHistory getBucketHistory() {
        return bucketHistory;
    }

}