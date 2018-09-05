package notes;

import java.util.HashMap;
import java.util.HashSet;

public class NoteSnapshot {
    public final HashSet<Note> liveNotes;

    public NoteSnapshot(){
        liveNotes = new HashSet<>();
    }

    public NoteSnapshot(NoteSnapshot noteSnapshot) {
        liveNotes = new HashSet<>(noteSnapshot.liveNotes);
    }

    public NoteSnapshot(HashSet<Note> liveNotes) {
        this.liveNotes = liveNotes;
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