package mixer.envelope;

import mixer.envelope.functions.EnvelopeFunction;
import sound.SampleRate;
import time.TimeInSeconds;

import java.util.Arrays;

public class SimpleEnvelope implements Envelope {
    private final EnvelopeFunction envelopeFunction;
    private final SampleRate sampleRate;
    private final long startingSampleCount;

    SimpleEnvelope(long startingSampleCount, SampleRate sampleRate, EnvelopeFunction envelopeFunction) {
        this.sampleRate = sampleRate;
        this.startingSampleCount = startingSampleCount;
        this.envelopeFunction = envelopeFunction;
    }

    double getVolume(long startingSampleCount, long sampleCount) {
        if (sampleCount < startingSampleCount) {
            return 0.;
        } else {
            return envelopeFunction.getVolume(getTimeDifference(startingSampleCount, sampleCount));
        }
    }

    public double getVolume(long sampleCount) {
        return getVolume(startingSampleCount, sampleCount);
    }

    TimeInSeconds getTimeDifference(long startingSampleCount, long sampleCount) {
        return sampleRate.asTime(sampleCount - startingSampleCount);
    }

    TimeInSeconds getTimeDifference(long sampleCount) {
        return getTimeDifference(startingSampleCount, sampleCount);
    }

    public Envelope add(Envelope envelope) {
        return new CompositeEnvelope<>(Arrays.asList(this, envelope));
    }

    @Override
    public Envelope update(long sampleCount) {
        return this;
    }

    public Envelope add(CompositeEnvelope envelope) {
        return envelope.add(this);
    }

    public long getStartingSampleCount() {
        return startingSampleCount;
    }

    public EnvelopeFunction getEnvelopeFunction() {
        return envelopeFunction;
    }

    public SampleRate getSampleRate() {
        return sampleRate;
    }
}
