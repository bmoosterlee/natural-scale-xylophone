package notes.state;

import main.SampleRate;
import notes.Note;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class NoteState {
    public final HashSet<Note> notes;

    public NoteState(){
        notes = new HashSet<>();
    }

    public NoteState(HashSet<Note> notes) {
        this.notes = notes;
    }

    public NoteState addNote(Note note) {
        HashSet<Note> newLiveNotes = new HashSet<>(notes);
        newLiveNotes.add(note);
        return new NoteState(newLiveNotes);
    }

    public NoteState removeNote(Note note) {
        HashSet<Note> newLiveNotes = new HashSet<>(notes);
        newLiveNotes.remove(note);
        return new NoteState(newLiveNotes);
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