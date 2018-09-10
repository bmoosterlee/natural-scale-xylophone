package notes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class NoteSnapshot {
    public final HashSet<Note> liveNotes;

    public NoteSnapshot(){
        liveNotes = new HashSet<>();
    }

    public NoteSnapshot(HashSet<Note> liveNotes) {
        this.liveNotes = liveNotes;
    }

    public NoteSnapshot addNote(Note note) {
        HashSet<Note> liveNotes = new HashSet<>(this.liveNotes);
        liveNotes.add(note);
        return new NoteSnapshot(liveNotes);
    }

    public NoteSnapshot removeInaudibleNotes(Set<Note> inaudibleNotes) {
        HashSet<Note> liveNotes = new HashSet<>(this.liveNotes);
        liveNotes.removeAll(inaudibleNotes);
        return new NoteSnapshot(liveNotes);
    }


    public HashMap<Note, Double> getVolumeTable(long sampleCount) {
        HashMap<Note, Double> volumeTable = new HashMap<>();
        for(Note note : liveNotes) {
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