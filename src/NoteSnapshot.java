import java.util.HashMap;
import java.util.HashSet;

public class NoteSnapshot {
    HashSet<Note> liveNotes;
    HashMap<Note, Envelope> envelopes;

    public NoteSnapshot() {
        liveNotes = new HashSet();
        envelopes = new HashMap<>();
    }

    public NoteSnapshot(NoteSnapshot noteSnapshot) {
        liveNotes = new HashSet<>(noteSnapshot.liveNotes);
        envelopes = new HashMap<>(noteSnapshot.envelopes);
    }
}