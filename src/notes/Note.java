package notes;

import frequency.Frequency;
import notes.envelope.Envelope;

public class Note {
    private final Frequency frequency;
    private final Envelope envelope;

    public Note(Frequency frequency, Envelope envelope){
        this.frequency = frequency;
        this.envelope = envelope;
    }

    public Frequency getFrequency() {
        return frequency;
    }

    public Envelope getEnvelope() {
        return envelope;
    }

    public boolean isDead(long sampleCount) {
        return getEnvelope().isDead(sampleCount);
    }
}
