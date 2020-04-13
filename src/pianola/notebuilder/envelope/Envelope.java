package notebuilder.envelope;

import notebuilder.envelope.functions.EnvelopeFunction;
import sound.SampleRate;

public interface Envelope {

    long getStartingSampleCount();
    EnvelopeFunction getEnvelopeFunction();
    SampleRate getSampleRate();

    double getVolume(long sampleCount);

    Envelope add(Envelope envelope);

    Envelope update(long sampleCount);

}
