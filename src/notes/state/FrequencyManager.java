package notes.state;

import frequency.FrequencyState;
import frequency.SimpleFrequencyState;

public class FrequencyManager {
    private final NoteManager noteManager;
    FrequencyState frequencyState;

    public FrequencyManager(NoteManager noteManager) {
        this.noteManager = noteManager;
        frequencyState = new SimpleFrequencyState();
    }

    public FrequencyState getFrequencyState(long sampleCount) {
        synchronized (noteManager) {
            frequencyState = frequencyState.update(noteManager.getNoteState(sampleCount).getNotes());
            frequencyState = frequencyState.update(sampleCount);
            return frequencyState;
        }
    }
}