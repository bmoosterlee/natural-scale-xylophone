package mixer;

import frequency.Frequency;

import java.util.Collection;

public class NewNoteAmplitudeData {
    private final Long sampleCount;
    private final Long endingSampleCount;
    private final Collection<Frequency> newNotes;

    public NewNoteAmplitudeData(Long sampleCount, Long endingSampleCount, Collection<Frequency> newNotes) {
        this.sampleCount = sampleCount;
        this.endingSampleCount = endingSampleCount;
        this.newNotes = newNotes;
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
}
