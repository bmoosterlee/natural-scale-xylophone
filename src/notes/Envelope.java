package notes;

import main.SampleRate;

public abstract class Envelope {
    protected final SampleRate sampleRate;
    protected double amplitude;
    protected long startingSampleCount;

    public Envelope(long startingSampleCount, SampleRate sampleRate){
        this.sampleRate = sampleRate;
        this.startingSampleCount = startingSampleCount;
    }

    public Envelope(long startingSampleCount, SampleRate sampleRate, double amplitude) {
        this(startingSampleCount, sampleRate);
        this.amplitude = amplitude;
    }

    public double getVolume(long startingSampleCount, long sampleCount) {
        if (sampleCount < startingSampleCount) {
            return 0;
        }

        return getVolume(getTimeDifference(startingSampleCount, sampleCount));
    }

    public double getVolume(long sampleCount) {
        return getVolume(startingSampleCount, sampleCount);
    }

    protected abstract double getVolume(double timeDifference);

    double getVolumeAsymptotic(double amplitude, double multiplier, double timeDifference) {
        return amplitude / (timeDifference *multiplier + 1);
    }

    double getTimeDifference(long startingSampleCount, long sampleCount) {
        return sampleRate.asTime(sampleCount - startingSampleCount);
    }

    double getTimeDifference(long sampleCount) {
        return getTimeDifference(startingSampleCount, sampleCount);
    }

}