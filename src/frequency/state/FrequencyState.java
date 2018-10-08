package frequency.state;

import frequency.Frequency;
import notes.Note;

import java.util.*;






public interface FrequencyState {

    FrequencyState update(Set<Note> notes);

    FrequencyState update(long sampleCount);

    FrequencyState addNote(Note note);

    FrequencyState removeNote(Note note);

    Set<Frequency> getFrequencies();

    Double getVolume(Frequency frequency, long sampleCount);

    Map<Frequency, Double> getFrequencyVolumeTable(long sampleCount);

}