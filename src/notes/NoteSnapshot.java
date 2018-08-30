package notes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class NoteSnapshot {
    public final long sampleCount;
    public final HashSet<Note> liveNotes;
    public final HashMap<Note, Envelope> envelopes;

    public NoteSnapshot(long sampleCount){
        this.sampleCount = sampleCount;
        liveNotes = new HashSet<>();
        envelopes = new HashMap<>();
    }

    public NoteSnapshot(long sampleCount, NoteSnapshot noteSnapshot) {
        this.sampleCount = sampleCount;
        liveNotes = new HashSet<>(noteSnapshot.liveNotes);
        envelopes = new HashMap<>(noteSnapshot.envelopes);
    }

    public HashMap<Note, Double> getVolumeTable() {
        HashMap<Note, Double> volumeTable = new HashMap<>();
        for(Note note : liveNotes) {
            double volume;
            try {
                volume = envelopes.get(note).getVolume(sampleCount);
            }
            catch(NullPointerException e){
                volume = 0.0;
            }
            volumeTable.put(note, volume);
        }
        return volumeTable;
    }
}