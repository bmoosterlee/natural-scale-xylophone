package notes.state;

import notes.Note;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class NoteState {
    public final HashSet<Note> notes;
    public final FrequencyState frequencyState;

    public NoteState(){
        notes = new HashSet<>();
        frequencyState = new FrequencyState();
    }

    public NoteState(HashSet<Note> notes, FrequencyState frequencyState) {
        this.notes = notes;
        this.frequencyState = frequencyState;
    }

    public NoteState addNote(Note note) {
        HashSet<Note> newLiveNotes = new HashSet<>(notes);
        newLiveNotes.add(note);
        return new NoteState(newLiveNotes, frequencyState.addNote(note));
    }

    public NoteState removeInaudibleNotes(Set<Note> inaudibleNotes) {
        HashSet<Note> newLiveNotes = new HashSet<>(notes);
        newLiveNotes.removeAll(inaudibleNotes);
        return new NoteState(newLiveNotes, frequencyState.removeInaudibleNotes(inaudibleNotes));
    }

    public HashMap<Note, Double> getVolumeTable(long sampleCount) {
        return getVolumeTable(sampleCount, this.notes);
    }

    protected static HashMap<Note, Double> getVolumeTable(long sampleCount, HashSet<Note> notes) {
        HashMap<Note, Double> volumeTable = new HashMap<>();
        for(Note note : notes) {
            double volume;
            try {
                volume = note.getEnvelope().getVolume(sampleCount);
            }
            catch(NullPointerException e){
                volume = 0.0;
            }
            volumeTable.put(note, volume);
        }
        return volumeTable;
    }
}