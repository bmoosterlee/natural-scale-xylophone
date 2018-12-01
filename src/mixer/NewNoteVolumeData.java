package mixer;

import frequency.Frequency;
import mixer.envelope.DeterministicEnvelope;

import java.util.Collection;

public class NewNoteVolumeData {
    private final Long sampleCount;
    private final Long endingSampleCount;
    private final Collection<Frequency> newNotes;
    private final DeterministicEnvelope envelope;

    public NewNoteVolumeData(Long sampleCount, Long endingSampleCount, Collection<Frequency> newNotes, DeterministicEnvelope envelope) {
        this.sampleCount = sampleCount;
        this.endingSampleCount = endingSampleCount;
        this.newNotes = newNotes;
        this.envelope = envelope;
    }

    public Long getSampleCount() {
        return sampleCount;
    }

    public Long getEndingSampleCount() {
        return endingSampleCount;
    }

    public Collection<Frequency> getNewNotes() {
        return newNotes;
    }

    public DeterministicEnvelope getEnvelope() {
        return envelope;
    }
}
