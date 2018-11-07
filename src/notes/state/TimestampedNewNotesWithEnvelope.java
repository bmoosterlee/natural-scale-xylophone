package notes.state;

import frequency.Frequency;
import notes.envelope.DeterministicEnvelope;

import java.util.Collection;

public class TimestampedNewNotesWithEnvelope {
    private final Long sampleCount;
    private final DeterministicEnvelope envelope;
    private final Collection<Frequency> frequencies;

    public TimestampedNewNotesWithEnvelope(Long sampleCount, DeterministicEnvelope envelope, Collection<Frequency> frequencies) {
        this.sampleCount = sampleCount;
        this.envelope = envelope;
        this.frequencies = frequencies;
    }

    public long getSampleCount() {
        return sampleCount;
    }

    public DeterministicEnvelope getEnvelope() {
        return envelope;
    }

    public Collection<Frequency> getFrequencies() {
        return frequencies;
    }
}
