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
        frequencyState = new SimpleFrequencyState();
        waveState = new WaveState(sampleRate);
    }

    void removeNote(Note note) {
        synchronized (noteState) {
            noteState = noteState.removeNote(note);
            frequencyState = frequencyState.removeNote(note);
            waveState = waveState.update(frequencyState.getFrequencies());
        }
    }

    public void addNote(Frequency frequency) {
        Note note = noteEnvironment.createNote(frequency);

        synchronized (noteState) {
            noteState = noteState.addNote(note);
            frequencyState = frequencyState.addNote(note);
            waveState = waveState.update(frequencyState.getFrequencies());
        }
    }

    public NoteSnapshot getSnapshot() {
        synchronized (noteState) {
            return new NoteSnapshot(noteState, frequencyState, waveState);
        }
    }
}