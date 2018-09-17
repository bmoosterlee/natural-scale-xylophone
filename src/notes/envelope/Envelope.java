package notes.envelope;

import main.SampleRate;
import notes.envelope.functions.EnvelopeFunction;

import java.util.Arrays;

public interface Envelope {

    public long getStartingSampleCount();
    public EnvelopeFunction getEnvelopeFunction();
    public SampleRate getSampleRate();

    double getVolume(long sampleCount);

    Envelope add(Envelope envelope);

    Envelope update(long sampleCount);

    boolean isDead(long sampleCount);
}
