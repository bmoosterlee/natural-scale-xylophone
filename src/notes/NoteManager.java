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
                noteSnapshot = noteSnapshot.removeInaudibleNotes(inaudibleNotes);
                frequencySnapshot = frequencySnapshot.removeInaudibleNotes(inaudibleNotes);
            }
        }
    }

    public void addNote(Frequency frequency) {
        Note note = noteEnvironment.createNote(frequency);

        synchronized (noteSnapshot) {
            synchronized (frequencySnapshot) {
                noteSnapshot = noteSnapshot.addNote(note);
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