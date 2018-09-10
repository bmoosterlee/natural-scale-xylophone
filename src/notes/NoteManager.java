package notes;

import java.util.*;

public class NoteManager {
    private final NoteEnvironment noteEnvironment;
    private NoteState noteState;

    public NoteManager(NoteEnvironment noteEnvironment) {
        this.noteEnvironment = noteEnvironment;
        noteState = new NoteState();
    }

    void removeInaudibleNotes(Set<Note> inaudibleNotes) {
        synchronized (noteState) {
                noteState = noteState.removeInaudibleNotes(inaudibleNotes);
        }
    }

    public void addNote(Frequency frequency) {
        Note note = noteEnvironment.createNote(frequency);

        synchronized (noteState) {
                noteState = noteState.addNote(note);
        }
    }

    public NoteState getSnapshot() {
        return noteState;
    }

}