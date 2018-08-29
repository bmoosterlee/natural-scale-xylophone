import java.util.HashMap;
import java.util.HashSet;

public class NoteFrequencySnapshot {
    final NoteSnapshot noteSnapshot;
    final FrequencySnapshot frequencySnapshot;

    public NoteFrequencySnapshot(NoteSnapshot noteSnapshot, FrequencySnapshot frequencySnapshot) {
        this.noteSnapshot = noteSnapshot;
        this.frequencySnapshot = frequencySnapshot;
    }
}
