import javafx.util.Pair;

import java.util.*;

public class HarmonicBuffer {
    PriorityQueue<Harmonic> harmonicHierarchy;
    HashMap<Harmonic, Double> volumeTable;
    HashMap<Note, HashSet<Harmonic>> harmonicsTable;

    public HarmonicBuffer() {
        volumeTable = new HashMap<>();

        harmonicsTable = new HashMap<>();
    }

    void addHarmonicToHarmonicBuffer(double newHarmonicVolume, Note highestValueNote, Harmonic highestValueHarmonic) {
        synchronized (volumeTable) {
            volumeTable.put(highestValueHarmonic, newHarmonicVolume);
        }
        synchronized (harmonicHierarchy) {
            harmonicHierarchy.add(highestValueHarmonic);
        }

        synchronized(harmonicsTable) {
            harmonicsTable.get(highestValueNote).add(highestValueHarmonic);
        }
    }

    double getHighestPriorityHarmonicVolume(int maxHarmonics) {
        try {
            return volumeTable.get(harmonicHierarchy.toArray(new Harmonic[harmonicHierarchy.size()])[maxHarmonics]);
        }
        catch(ArrayIndexOutOfBoundsException e){
            return 0;
        }
    }

    PriorityQueue<Harmonic> buildHarmonicHierarchy(HashMap<Note, Double> noteVolumeTable) {
//            calculate harmonic volumes using note volumes
//            store volumes in a pair with the harmonic in a priorityqueue

        PriorityQueue<Harmonic> newHarmonicPriorityQueue = new PriorityQueue<>((o1, o2) -> -volumeTable.get(o1).compareTo(volumeTable.get(o2)));
        synchronized (harmonicsTable) {
            for(Map.Entry<Note,Double> pair : new HashSet<>(noteVolumeTable.entrySet())){
                HashSet<Harmonic> harmonics = harmonicsTable.get(pair.getKey());
                for (Harmonic harmonic : harmonics) {
                    newHarmonicPriorityQueue.add(harmonic);
                }
            }
        }
        return newHarmonicPriorityQueue;
    }

    void rebuildHarmonicHierarchy(HashMap<Note, Double> volumeTable) {
        synchronized(harmonicsTable) {
            harmonicsTable = updateHarmonicsTable(volumeTable.keySet());
        }
        harmonicHierarchy = buildHarmonicHierarchy(volumeTable);
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

    public LinkedList<Pair<Harmonic, Double>> getHarmonicHierarchy() {
        LinkedList<Harmonic> harmonicHierarchy = new LinkedList<>(this.harmonicHierarchy);
        LinkedList<Pair<Harmonic, Double>> harmonicsWithVolumes = new LinkedList<>();
        for(Harmonic harmonic : harmonicHierarchy){
            harmonicsWithVolumes.addLast(new Pair(harmonic, volumeTable.get(harmonic)));
        }
        return harmonicsWithVolumes;
    }
}