package notes;

import java.util.Map;

public class NoteFrequencySnapshot {
    public final NoteState noteState;
    public final FrequencyState frequencyState;

    public NoteFrequencySnapshot(NoteState noteState, FrequencyState frequencyState) {
        this.noteState = noteState;
        this.frequencyState = frequencyState;
    }


}
