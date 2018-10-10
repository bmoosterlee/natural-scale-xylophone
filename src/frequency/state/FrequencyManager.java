package frequency.state;

import notes.state.NoteManager;

public class FrequencyManager {
    private final NoteManager noteManager;

    FrequencyState frequencyState;
    long updatedToSample = -1;

    public FrequencyManager(NoteManager noteManager) {
        this.noteManager = noteManager;
        frequencyState = new FrequencyState();
    }

    public FrequencyState getFrequencyState(long sampleCount) {
        synchronized (noteManager) {
            if(sampleCount>updatedToSample) {
                frequencyState = frequencyState.update(noteManager.getNoteState(sampleCount).getNotes());
                frequencyState = frequencyState.update(sampleCount);
                updatedToSample = sampleCount;
            }
            return frequencyState;
        }
    }
}