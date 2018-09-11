package notes.state;

import main.SampleRate;
import notes.Frequency;
import notes.Note;

import java.util.*;

public class NoteManager {
    private final NoteEnvironment noteEnvironment;
    private NoteState noteState;
    public FrequencyState frequencyState;
    private WaveState waveState;

    public NoteManager(NoteEnvironment noteEnvironment, SampleRate sampleRate) {
        this.noteEnvironment = noteEnvironment;
        noteState = new NoteState();
        frequencyState = new FrequencyState();
        waveState = new WaveState(sampleRate);
    }

    void removeInaudibleNotes(Set<Note> inaudibleNotes) {
        synchronized (noteState) {
            noteState = noteState.removeNotes(inaudibleNotes);
            frequencyState = frequencyState.removeNotes(inaudibleNotes);
            waveState = waveState.update(frequencyState.frequencies);
        }
    }

    public void addNote(Frequency frequency) {
        Note note = noteEnvironment.createNote(frequency);

        synchronized (noteState) {
            noteState = noteState.addNote(note);
            frequencyState = frequencyState.addNote(note);
            waveState = waveState.update(frequencyState.frequencies);
        }
    }

    public NoteSnapshot getSnapshot() {
        synchronized (noteState) {
            return new NoteSnapshot(noteState, frequencyState, waveState);
        }
    }
}