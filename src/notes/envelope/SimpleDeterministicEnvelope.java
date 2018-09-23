package notes.envelope;

import notes.envelope.functions.DeterministicFunction;
import sound.SampleRate;

import java.util.Arrays;

public class SimpleDeterministicEnvelope extends SimpleEnvelope implements DeterministicEnvelope {
    private final long endingSampleCount;

    public SimpleDeterministicEnvelope(long startingSampleCount, SampleRate sampleRate, DeterministicFunction deterministicFunction) {
        super(startingSampleCount, sampleRate, deterministicFunction, null);
        endingSampleCount = startingSampleCount+sampleRate.asSampleCount(deterministicFunction.totalTime);
    }

    public SimpleDeterministicEnvelope(long startingSampleCount, SampleRate sampleRate, long endingSampleCount) {
        super(startingSampleCount, sampleRate, null, null);
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

    public DeterministicCompositeEnvelope add(DeterministicEnvelope envelope) {
        return new DeterministicCompositeEnvelope(Arrays.asList(new DeterministicEnvelope[]{this, envelope}));
    }

    public DeterministicCompositeEnvelope add(DeterministicCompositeEnvelope envelope) {
        return envelope.add(this);
    }

    @Override
    public Envelope update(long sampleCount){
        // precalculate();
        //todo time to implement the time data structure with the nested lists and a timestamp.
        //todo we then grab the structure from the precalculated version and return that.

        if(isDead(sampleCount)){
            return null;
        }
        else{
            return this;
        }
    }

    @Override
    public boolean isDead(long sampleCount) {
        return sampleCount >= getEndingSampleCount();
    }
}
