package notes;

import javafx.util.Pair;

import java.util.*;

public class NoteManager {
    private final NoteEnvironment noteEnvironment;
    private NoteSnapshot noteSnapshot;
    private FrequencySnapshot frequencySnapshot;

    public NoteManager(NoteEnvironment noteEnvironment) {
        this.noteEnvironment = noteEnvironment;
        noteSnapshot = new NoteSnapshot(0L);
        frequencySnapshot = new FrequencySnapshot(0);
    }

    void removeInaudibleNotes(Set<Note> inaudibleNotes) {
        NoteSnapshot newNoteSnapshot = new NoteSnapshot(noteSnapshot.sampleCount, noteSnapshot);
        synchronized (noteSnapshot) {
            newNoteSnapshot.liveNotes.removeAll(inaudibleNotes);
            Iterator<Note> iterator = inaudibleNotes.iterator();
            while (iterator.hasNext()) {
                newNoteSnapshot.envelopes.remove(iterator.next());
            }
            noteSnapshot = newNoteSnapshot;
        }

        synchronized (frequencySnapshot) {
            frequencySnapshot = frequencySnapshot.removeInaudibleNotes(inaudibleNotes);
        }
    }

    public void addNote(double frequency) {
        Pair<Note, Envelope> pair = noteEnvironment.createNote();
        Note note = pair.getKey();
        Envelope envelope = pair.getValue();

        NoteSnapshot newNoteSnapshot = new NoteSnapshot(noteSnapshot.sampleCount, noteSnapshot);
        synchronized (noteSnapshot) {
            newNoteSnapshot.liveNotes.add(note);
            newNoteSnapshot.envelopes.put(note, envelope);
            noteSnapshot = newNoteSnapshot;
        }

        synchronized (frequencySnapshot) {
            frequencySnapshot = frequencySnapshot.addNote(note, frequency);
        }
    }

    public NoteFrequencySnapshot getSnapshot(long sampleCount) {
        synchronized (noteSnapshot) {
            synchronized (frequencySnapshot) {
                return new NoteFrequencySnapshot(sampleCount, new NoteSnapshot(sampleCount, noteSnapshot), new FrequencySnapshot(sampleCount, frequencySnapshot));
            }
        }
    }

}