package mixer.envelope;

import mixer.envelope.functions.DeterministicFunction;
import sound.SampleRate;

import java.util.Arrays;

public class SimpleDeterministicEnvelope extends SimpleEnvelope implements DeterministicEnvelope {
    private final long endingSampleCount;

    public SimpleDeterministicEnvelope(long startingSampleCount, SampleRate sampleRate, DeterministicFunction deterministicFunction) {
        super(startingSampleCount, sampleRate, deterministicFunction);
        endingSampleCount = startingSampleCount+sampleRate.asSampleCount(deterministicFunction.totalTime);
    }

    SimpleDeterministicEnvelope(long startingSampleCount, SampleRate sampleRate, long endingSampleCount) {
        super(startingSampleCount, sampleRate, null);
        this.endingSampleCount = endingSampleCount;
    }

    @Override
    protected double getVolume(long startingSampleCount, long sampleCount) {
        if(sampleCount<startingSampleCount || sampleCount>endingSampleCount){
            return 0.;
        }
        else {
            return getEnvelopeFunction().getVolume(getTimeDifference(startingSampleCount, sampleCount));
        }
    }

    public long getEndingSampleCount() {
        return endingSampleCount;
    }

    @Override
    public DeterministicCompositeEnvelope add(DeterministicEnvelope envelope) {
        return new DeterministicCompositeEnvelope(Arrays.asList(this, envelope));
    }

    public DeterministicCompositeEnvelope add(DeterministicCompositeEnvelope envelope) {
        return envelope.add(this);
    }

    @Override
    public Envelope update(long sampleCount){
        if(isDead(sampleCount)){
            return null;
        }
        else{
            return this;
        }
    }

    boolean isDead(long sampleCount) {
        return sampleCount >= getEndingSampleCount();
    }
}
