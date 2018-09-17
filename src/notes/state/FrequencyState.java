package notes.state;

import notes.Frequency;
import notes.Note;

import java.util.*;

public interface FrequencyState {

    FrequencyState addNote(Note note);

    public Set<Frequency> getFrequencies();

    FrequencyState removeNote(Note note);

    public Map<Frequency, Double> getFrequencyVolumeTable(long sampleCount);

    Double getVolume(Frequency frequency, long sampleCount);

    public FrequencyState update(Set<Note> notes);

    FrequencyState update(long sampleCount);
}