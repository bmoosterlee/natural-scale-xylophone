package notes.envelope;

import main.SampleRate;
import notes.envelope.functions.EnvelopeFunction;

import java.util.Arrays;
import java.util.HashSet;

public abstract class Envelope {
    protected final EnvelopeFunction envelopeFunction;
    protected final SampleRate sampleRate;
    protected final long startingSampleCount;

    public Envelope(long startingSampleCount, SampleRate sampleRate, EnvelopeFunction envelopeFunction) {
        this.sampleRate = sampleRate;
        this.startingSampleCount = startingSampleCount;
        this.envelopeFunction = envelopeFunction;
    }

    protected double getVolume(long startingSampleCount, long sampleCount) {
        if (sampleCount < startingSampleCount) {
            return 0.;
        } else {
            return envelopeFunction.getVolume(getTimeDifference(startingSampleCount, sampleCount));
        }
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

    public Envelope add(Envelope envelope) {
        return new CompositeEnvelope(Arrays.asList(new Envelope[]{this, envelope}));
    }

    public Envelope add(CompositeEnvelope envelope) {
        return envelope.add(this);
    }
}
