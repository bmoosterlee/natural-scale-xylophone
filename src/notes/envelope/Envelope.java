package notes.envelope;

import notes.envelope.functions.EnvelopeFunction;
import sound.SampleRate;

public interface Envelope {

    public long getStartingSampleCount();
    public EnvelopeFunction getEnvelopeFunction();
    public SampleRate getSampleRate();

    double getVolume(long sampleCount);

    Envelope add(Envelope envelope);

    Envelope update(long sampleCount);

    boolean isDead(long sampleCount);
}
