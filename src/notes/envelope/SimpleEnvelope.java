package notes.envelope;

import notes.envelope.functions.EnvelopeFunction;
import sound.SampleRate;
import sound.SoundEnvironment;
import time.TimeInSeconds;

import java.util.Arrays;

public class SimpleEnvelope implements Envelope {
    private final EnvelopeFunction envelopeFunction;
    private final SampleRate sampleRate;
    private final long startingSampleCount;
    private final SoundEnvironment soundEnvironment;

    public SimpleEnvelope(long startingSampleCount, SampleRate sampleRate, EnvelopeFunction envelopeFunction, SoundEnvironment soundEnvironment) {
        this.sampleRate = sampleRate;
        this.startingSampleCount = startingSampleCount;
        this.envelopeFunction = envelopeFunction;
        this.soundEnvironment = soundEnvironment;
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

    TimeInSeconds getTimeDifference(long startingSampleCount, long sampleCount) {
        return sampleRate.asTime(sampleCount - startingSampleCount);
    }

    TimeInSeconds getTimeDifference(long sampleCount) {
        return getTimeDifference(startingSampleCount, sampleCount);
    }

    public Envelope add(Envelope envelope) {
        return new CompositeEnvelope(Arrays.asList(new Envelope[]{this, envelope}));
    }

    @Override
    public Envelope update(long sampleCount) {
        if(isDead(sampleCount)){
            return null;
        }
        else {
            return this;
        }
    }

    @Override
    public boolean isDead(long sampleCount) {
        return !soundEnvironment.isAudible(getVolume(sampleCount));
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
