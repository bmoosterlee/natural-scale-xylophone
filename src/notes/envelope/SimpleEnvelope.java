package notes.envelope;

import main.SampleRate;
import notes.envelope.functions.EnvelopeFunction;

public class SimpleEnvelope extends Envelope {

    public SimpleEnvelope(long startingSampleCount, SampleRate sampleRate, EnvelopeFunction envelopeFunction) {
        super(startingSampleCount, sampleRate, envelopeFunction);
    }
}
