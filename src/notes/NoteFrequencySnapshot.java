package notes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NoteFrequencySnapshot {
    public final long sampleCount;
    public final NoteSnapshot noteSnapshot;
    public final FrequencySnapshot frequencySnapshot;

    public NoteFrequencySnapshot(long sampleCount, NoteSnapshot noteSnapshot, FrequencySnapshot frequencySnapshot) {
        this.sampleCount = sampleCount;
        this.noteSnapshot = noteSnapshot;
        this.frequencySnapshot = frequencySnapshot;
    }

    public Map<Double, Double> getFrequencyVolumeTable() {
        return getFrequencyVolumeTable(noteSnapshot.getVolumeTable());
    }

    public Map<Double, Double> getFrequencyVolumeTable(Map<Note, Double> volumeTable) {
        return frequencySnapshot.getFrequencyVolumeTable(volumeTable);
    }
}
