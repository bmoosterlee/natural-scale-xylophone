package gui;

import frequency.Frequency;
import harmonics.HarmonicCalculator;
import notes.state.FrequencyManager;
import notes.state.NoteManager;

import java.util.*;

public class SpectrumWindow {
    public FrequencyManager frequencyManager;
    public HarmonicCalculator harmonicCalculator;
    //todo move noteManager and harmonicCalculator to Renderer. Pass them during building.

    //todo create CachedBuckets class which store what harmony a bucket refers to. Pick the highest value one.
    //todo might just store only the highest one.
    //todo first step is to get X out of the bucket world. Start dealing with frequencies.
    //todo one way to do this is to keep a link between the frequencies that buckets were built on.
    //todo then we don't need to translate back and forth. The issue is that buckets which cache the real value
    //todo will not keep the frequencies, but the translated frequencies.
    //todo we probably should just move to frequency buckets.
    //todo we might index frequencies of buckets though such that we can still do easy manipulations on buckets using
    //todo these indices

    private final BucketHistory bucketHistory = new PrecalculatedBucketHistory(300);
    public final Frequency centerFrequency = new Frequency(2 * 261.63);
    double octaveRange = 3.;
    public Frequency lowerBound;
    public Frequency upperBound;
    double logFrequencyMultiplier;
    double logFrequencyAdditive;
    double xMultiplier;

    public SpectrumWindow(FrequencyManager frequencyManager, HarmonicCalculator harmonicCalculator) {
        this.frequencyManager = frequencyManager;
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

    SpectrumStateBuilder createBuilder(long sampleCount) {
        return new SpectrumStateBuilder(sampleCount, this);
    }

    boolean inBounds(Frequency frequency) {
        int x = getX(frequency);
        if (x < 0 || x >= gui.GUI.WIDTH) {
            return false;
        }
        return true;
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