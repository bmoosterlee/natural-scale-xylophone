package notes;

import javafx.util.Pair;

import java.util.*;

public class NoteManager {
    private final NoteEnvironment noteEnvironment;
    private NoteSnapshot noteSnapshot;
    private FrequencySnapshot frequencySnapshot;

    public NoteManager(NoteEnvironment noteEnvironment) {
        this.noteEnvironment = noteEnvironment;
        noteSnapshot = new NoteSnapshot();
        frequencySnapshot = new FrequencySnapshot();
    }

    void removeInaudibleNotes(Set<Note> inaudibleNotes) {
        synchronized (noteSnapshot) {
            synchronized (frequencySnapshot) {
                HashSet<Note> liveNotes = new HashSet<>(noteSnapshot.liveNotes);
                liveNotes.removeAll(inaudibleNotes);
                NoteSnapshot newNoteSnapshot = new NoteSnapshot(liveNotes);
                noteSnapshot = newNoteSnapshot;

                frequencySnapshot = frequencySnapshot.removeInaudibleNotes(inaudibleNotes);
            }
        }
    }

    public void addNote(double frequency) {
        Note note = noteEnvironment.createNote(frequency);

        synchronized (noteSnapshot) {
            synchronized (frequencySnapshot) {
                HashSet<Note> liveNotes = new HashSet<>(noteSnapshot.liveNotes);
                liveNotes.add(note);
                noteSnapshot = new NoteSnapshot(liveNotes);

                frequencySnapshot = frequencySnapshot.addNote(note);
            }
        }
    }

    public NoteFrequencySnapshot getSnapshot() {
        synchronized (noteSnapshot) {
            synchronized (frequencySnapshot) {
                return new NoteFrequencySnapshot(noteSnapshot, new FrequencySnapshot(frequencySnapshot));
            }
        }
    }

}