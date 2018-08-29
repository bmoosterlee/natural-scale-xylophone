import java.util.HashSet;

public class NoteSnapshot {
    final HashSet<Note> liveNotes;
    final FrequencySnapshot frequencySnapshot;

    public NoteSnapshot(HashSet<Note> liveNotes, FrequencySnapshot frequencySnapshot) {
        this.liveNotes = liveNotes;
        this.frequencySnapshot = frequencySnapshot;
    }
}
