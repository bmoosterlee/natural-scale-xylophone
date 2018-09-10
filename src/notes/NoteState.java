package notes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NoteState {
    public final HashSet<Note> notes;
    public FrequencyState frequencyState;

    public NoteState(){
        notes = new HashSet<>();
        frequencyState = new FrequencyState();
    }

    public NoteState(HashSet<Note> notes, FrequencyState frequencyState) {
        this.notes = notes;
        this.frequencyState = frequencyState;
    }

    public NoteState addNote(Note note) {
        HashSet<Note> liveNotes = new HashSet<>(this.notes);
        liveNotes.add(note);
        FrequencyState frequencyState = this.frequencyState.addNote(note);
        return new NoteState(liveNotes, frequencyState);
    }

    public NoteState removeInaudibleNotes(Set<Note> inaudibleNotes) {
        HashSet<Note> liveNotes = new HashSet<>(this.notes);
        liveNotes.removeAll(inaudibleNotes);
        FrequencyState frequencyState = this.frequencyState.removeInaudibleNotes(inaudibleNotes);
        return new NoteState(liveNotes, frequencyState);
    }

    public HashMap<Note, Double> getVolumeTable(long sampleCount) {
        return getVolumeTable(sampleCount, this.notes);
    }

    protected static HashMap<Note, Double> getVolumeTable(long sampleCount, HashSet<Note> notes) {
        HashMap<Note, Double> volumeTable = new HashMap<>();
        for(Note note : notes) {
            double volume;
            try {
                volume = note.envelope.getVolume(sampleCount);
            }
            catch(NullPointerException e){
                volume = 0.0;
            }
            volumeTable.put(note, volume);
        }
        return volumeTable;
    }
}