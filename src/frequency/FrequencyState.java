package frequency;

import notes.Note;

import java.util.*;

public interface FrequencyState {

    FrequencyState addNote(Note note);

    Set<Frequency> getFrequencies();

    FrequencyState removeNote(Note note);

    Map<Frequency, Double> getFrequencyVolumeTable(long sampleCount);

    Double getVolume(Frequency frequency, long sampleCount);

    FrequencyState update(Set<Note> notes);

    FrequencyState update(long sampleCount);

}