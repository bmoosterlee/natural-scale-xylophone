package notes;

import main.SampleRate;

public abstract class Envelope {
    protected final SampleRate sampleRate;
    protected final long startingSampleCount;

    public Envelope(long startingSampleCount, SampleRate sampleRate, EnvelopeFunction envelopeFunction) {
        this.sampleRate = sampleRate;
        this.startingSampleCount = startingSampleCount;
        this.envelopeFunction = envelopeFunction;
    }

    public double getVolume(long startingSampleCount, long sampleCount) {
        return envelopeFunction.getVolume(getTimeDifference(startingSampleCount, sampleCount));
    }

    public double getVolume(long sampleCount) {
        return getVolume(startingSampleCount, sampleCount);
    }

    double getTimeDifference(long startingSampleCount, long sampleCount) {
        return sampleRate.asTime(sampleCount - startingSampleCount);
    }

    double getTimeDifference(long sampleCount) {
        return getTimeDifference(startingSampleCount, sampleCount);
    }

}