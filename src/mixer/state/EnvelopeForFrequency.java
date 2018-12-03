package mixer.state;

import frequency.Frequency;
import mixer.envelope.Envelope;

public class EnvelopeForFrequency {
    private final Frequency frequency;
    private final Envelope envelope;

    public EnvelopeForFrequency() {
        this(null, null);
    }

    public EnvelopeForFrequency(Frequency frequency, Envelope envelope) {
        this.frequency = frequency;
        this.envelope = envelope;
    }

    public Frequency getFrequency() {
        return frequency;
    }

    public Envelope getEnvelope() {
        return envelope;
    }
}
