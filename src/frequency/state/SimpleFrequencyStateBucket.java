package frequency.state;

import notes.Note;

import java.util.HashSet;
import java.util.Set;

public class SimpleFrequencyStateBucket implements FrequencyStateBucket {

    final Set<Note> notes;

    public SimpleFrequencyStateBucket() {
        notes = new HashSet<>();
    }

    public SimpleFrequencyStateBucket(Set<Note> notes){
        this.notes = notes;
    }

    public Double getVolume(long sampleCount) {
        Double volume = 0.;
        for(Note note : notes){
            try {
                volume += note.getEnvelope().getVolume(sampleCount);
            } catch (NullPointerException e) {
                continue;
            }
        }
        return volume;
    }

    public SimpleFrequencyStateBucket removeNote(Note note) {
        Set<Note> newNotes = new HashSet<>(notes);
        newNotes.remove(note);
        if(newNotes.isEmpty()){
            return null;
        }
        else {
            return new SimpleFrequencyStateBucket(newNotes);
        }
    }

    public SimpleFrequencyStateBucket addNote(Note note) {
        Set<Note> newNotes = new HashSet<>(notes);
        newNotes.add(note);
        return new SimpleFrequencyStateBucket(newNotes);
    }
}
