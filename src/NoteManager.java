import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class NoteManager {
    private final NoteEnvironment noteEnvironment;
    HashSet<Note> liveNotes;

    public NoteManager(NoteEnvironment noteEnvironment) {
        this.noteEnvironment = noteEnvironment;

        liveNotes = new HashSet();
    }

    public HashMap<Note, Double> getVolumeTable(Set<Note> liveNotes) {
        return NoteEnvironment.getVolumeTable(noteEnvironment.getExpectedSampleCount(), liveNotes);
    }

    void removeInaudibleNotes(HashSet<Note> inaudibleNotes) {
        synchronized (liveNotes) {
            liveNotes.removeAll(inaudibleNotes);
        }
    }

    HashSet<Note> getInaudibleNotes(HashMap<Note, Double> volumeTable, HashSet<Note> liveNotes) {
        HashSet<Note> notesToBeRemoved = new HashSet<Note>();
        for (Note note : liveNotes) {
            if (!noteEnvironment.isAudible(volumeTable.get(note))) {
                notesToBeRemoved.add(note);
            }
        }
        return notesToBeRemoved;
    }

    public HashSet<Note> getLiveNotes() {
        synchronized (liveNotes) {
            return new HashSet<Note>(liveNotes);
        }
    }

    public void addNote(double frequency) {
        Note note = noteEnvironment.createNote(frequency);
        synchronized (liveNotes) {
            liveNotes.add(note);
        }
    }
}