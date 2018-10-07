package gui;

import frequency.Frequency;
import harmonics.HarmonicCalculator;
import notes.state.FrequencyManager;

public class SpectrumWindow {
    private final int width;
    FrequencyManager frequencyManager;
    HarmonicCalculator harmonicCalculator;
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

    public final Frequency centerFrequency = new Frequency(2 * 261.63);
    private final double octaveRange = 3.;
    public final Frequency lowerBound;
    public final Frequency upperBound;
    private final double logFrequencyMultiplier;
    private final double logFrequencyAdditive;
    private final double xMultiplier;

    SpectrumWindow(FrequencyManager frequencyManager, HarmonicCalculator harmonicCalculator, int width) {
        this.frequencyManager = frequencyManager;
        this.harmonicCalculator = harmonicCalculator;
        this.width = width;

        lowerBound = centerFrequency.divideBy(Math.pow(2, octaveRange / 2));
        upperBound = centerFrequency.multiplyBy(Math.pow(2, octaveRange / 2));

        double logLowerBound = Math.log(lowerBound.getValue());
        double logUpperBound = Math.log(upperBound.getValue());
        double logRange = logUpperBound - logLowerBound;
        logFrequencyMultiplier = this.width / logRange;
        logFrequencyAdditive = logLowerBound * this.width / logRange;
        xMultiplier = logRange / this.width;
    }

    SpectrumStateBuilder createBuilder(SpectrumState spectrumState, long sampleCount) {
        return new SpectrumStateBuilder(spectrumState, sampleCount, this);
    }

    boolean inBounds(Frequency frequency) {
        int x = getX(frequency);
        if (x < 0 || x >= width) {
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

}