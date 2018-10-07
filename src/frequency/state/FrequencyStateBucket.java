package frequency.state;

import notes.Note;

public interface FrequencyStateBucket {

    Double getVolume(long sampleCount);

    <T extends FrequencyStateBucket> T removeNote(Note note);

    <T extends FrequencyStateBucket> T addNote(Note note);
}
