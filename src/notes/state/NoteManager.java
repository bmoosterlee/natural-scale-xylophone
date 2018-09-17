package notes.state;

import main.SampleRate;
import notes.Frequency;
import notes.Note;

import java.util.*;

public class NoteManager {
    //todo move frequencyState elsewhere.
    //todo let frequencyState use an update method instead of add and removeNotes
    private final NoteEnvironment noteEnvironment;
    private NoteState noteState;
    private FrequencyState frequencyState;
    private WaveState waveState;

    public NoteManager(NoteEnvironment noteEnvironment, SampleRate sampleRate) {
        this.noteEnvironment = noteEnvironment;
        noteState = new NoteState();
        // frequencyState = new SimpleFrequencyState();
        frequencyState = new CompositeFrequencyState();
        waveState = new WaveState(sampleRate);
    }

    public void addNote(Frequency frequency) {
        Note note = noteEnvironment.createNote(frequency);

        synchronized (noteState) {
            noteState = noteState.addNote(note);
            frequencyState = frequencyState.update(noteState.getNotes());
            waveState = waveState.update(frequencyState.getFrequencies());
        }
    }

    public NoteSnapshot getSnapshot(long sampleCount) {
        synchronized (noteState) {
            noteState = noteState.update(sampleCount);
            frequencyState = frequencyState.update(sampleCount);
            waveState = waveState.update(sampleCount);
            return new NoteSnapshot(noteState, frequencyState, waveState);
        }
    }
}