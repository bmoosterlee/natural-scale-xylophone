package notes.state;

import frequency.FrequencyState;
import frequency.SimpleFrequencyState;
import frequency.SimpleFrequencyStateBucket;

public class FrequencyManager {
    private final NoteManager noteManager;
    FrequencyState frequencyState;

    public FrequencyManager(NoteManager noteManager) {
        this.noteManager = noteManager;
        frequencyState = new SimpleFrequencyState<>(SimpleFrequencyStateBucket::new);
    }

    public FrequencyState getFrequencyState(long sampleCount) {
        synchronized (noteManager) {
            frequencyState = frequencyState.update(noteManager.getNoteState(sampleCount).getNotes());
            frequencyState = frequencyState.update(sampleCount);
            return frequencyState;
        }
    }
}