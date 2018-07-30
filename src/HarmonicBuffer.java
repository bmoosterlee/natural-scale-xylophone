import java.util.*;

public class HarmonicBuffer {
    HashMap<Note, HashSet<Harmonic>> harmonicsTable;

    public HarmonicBuffer() {
        harmonicsTable = new HashMap<>();
    }

    void addHarmonicToHarmonicBuffer(double newHarmonicVolume, Note highestValueNote, Harmonic highestValueHarmonic, BufferSnapshot bufferSnapshot) {
        bufferSnapshot.addHarmonic(newHarmonicVolume, highestValueHarmonic);

        synchronized(harmonicsTable) {
            harmonicsTable.get(highestValueNote).add(highestValueHarmonic);
        }
    }

    BufferSnapshot rebuildHarmonicHierarchy(HashMap<Note, Double> noteVolumeTable) {
        synchronized(harmonicsTable) {
            harmonicsTable = updateHarmonicsTable(noteVolumeTable.keySet());
        }

        return new BufferSnapshot(harmonicsTable, noteVolumeTable);
    }

    private HashMap<Note, HashSet<Harmonic>> updateHarmonicsTable(Set<Note> liveNotes) {
        HashMap<Note, HashSet<Harmonic>> newHarmonicsTable = new HashMap<>();
        for (Note note : liveNotes) {
            if(harmonicsTable.containsKey(note)) {
                newHarmonicsTable.put(note, harmonicsTable.get(note));
            }
            else{
                newHarmonicsTable.put(note, new HashSet<>());
            }
        }
        return newHarmonicsTable;
    }

}