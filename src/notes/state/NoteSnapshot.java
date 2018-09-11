package notes.state;

import notes.Frequency;

public class NoteSnapshot {
    public final NoteState noteState;
    public final FrequencyState frequencyState;
    final WaveState waveState;

    public NoteSnapshot(NoteState noteState, FrequencyState frequencyState, WaveState waveState) {
        this.noteState = noteState;
        this.frequencyState = frequencyState;
        this.waveState = waveState;
    }

}
