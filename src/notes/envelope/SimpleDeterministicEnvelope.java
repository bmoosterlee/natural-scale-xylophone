package notes.envelope;

import main.SampleRate;
import notes.envelope.functions.DeterministicFunction;

public class SimpleDeterministicEnvelope extends DeterministicEnvelope{

    public SimpleDeterministicEnvelope(long startingSampleCount, SampleRate sampleRate, DeterministicFunction deterministicFunction) {
        super(startingSampleCount, sampleRate, deterministicFunction);
    }
}
