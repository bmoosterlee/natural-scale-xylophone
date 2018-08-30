package notes;

public class NoteFrequencySnapshot {
    public final NoteSnapshot noteSnapshot;
    public final FrequencySnapshot frequencySnapshot;

    public NoteFrequencySnapshot(NoteSnapshot noteSnapshot, FrequencySnapshot frequencySnapshot) {
        this.noteSnapshot = noteSnapshot;
        this.frequencySnapshot = frequencySnapshot;
    }
}
