package notes.envelope;

import main.SampleRate;
import notes.envelope.functions.DeterministicFunction;

public class DeterministicEnvelope extends Envelope {
    final long endingSampleCount;

    public DeterministicEnvelope(long startingSampleCount, SampleRate sampleRate, DeterministicFunction deterministicFunction) {
        super(startingSampleCount, sampleRate, deterministicFunction);
        endingSampleCount = startingSampleCount+sampleRate.asSampleCount(deterministicFunction.totalTime);
    }

    protected double getVolume(long startingSampleCount, long sampleCount) {
        if(sampleCount<startingSampleCount || sampleCount>endingSampleCount){
            return 0.;
        }
        else {
            return envelopeFunction.getVolume(getTimeDifference(startingSampleCount, sampleCount));
        }
    }

}
