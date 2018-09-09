package notes;

import java.util.Map;

public class NoteFrequencySnapshot {
    public final NoteSnapshot noteSnapshot;
    public final FrequencySnapshot frequencySnapshot;

    public NoteFrequencySnapshot(NoteSnapshot noteSnapshot, FrequencySnapshot frequencySnapshot) {
        this.noteSnapshot = noteSnapshot;
        this.frequencySnapshot = frequencySnapshot;
    }

    public Map<Frequency, Double> getFrequencyVolumeTable(long sampleCount) {
        return getFrequencyVolumeTable(noteSnapshot.getVolumeTable(sampleCount));
    }

    public Map<Frequency, Double> getFrequencyVolumeTable(Map<Note, Double> volumeTable) {
        return frequencySnapshot.getFrequencyVolumeTable(volumeTable);
    }
}
