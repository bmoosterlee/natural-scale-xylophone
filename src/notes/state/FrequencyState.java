package notes.state;

import notes.Frequency;
import notes.Note;

import java.util.*;

public interface FrequencyState {

    FrequencyState addNote(Note note);

    public Set<Frequency> getFrequencies();

    FrequencyState removeNote(Note note);

        //todo if we want the list to be immutable, we could calculate the death time of each note, and create a
        //todo new version of the object at each death time.
        //todo by using zipper like structures, we can only update a particular note list.
        //todo we can link together all these death time based frequencytables, and request the current one from the
        // todo noteManager, like an iterator. We might even turn it into an iterator.
    public Map<Frequency, Double> getFrequencyVolumeTable(long sampleCount);

    Double getVolume(Frequency frequency, long sampleCount);
}