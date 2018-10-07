package notes;

import frequency.Frequency;
import notes.envelope.Envelope;

public class Note<T extends Envelope> {
    private final Frequency frequency;
    private final T envelope;

    public Note(Frequency frequency, T envelope){
        if(frequency==null) throw new NullPointerException();

        this.frequency = frequency;
        this.envelope = envelope;
    }

    public Frequency getFrequency() {
        return frequency;
    }

    public T getEnvelope() {
        return envelope;
    }

    public boolean isDead(long sampleCount) {
        return getEnvelope().isDead(sampleCount);
    }
}
