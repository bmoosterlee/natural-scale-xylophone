package notebuilder;

import frequency.Frequency;
import notebuilder.envelope.DeterministicEnvelope;

import java.util.Collection;

public class NewNotesVolumeData {
    private final Long sampleCount;
    private final Long endingSampleCount;
    private final Collection<Frequency> newNotes;
    private final DeterministicEnvelope envelope;

    public NewNotesVolumeData(Long sampleCount, Long endingSampleCount, Collection<Frequency> newNotes, DeterministicEnvelope envelope) {
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
