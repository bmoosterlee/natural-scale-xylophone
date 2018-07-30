import javafx.util.Pair;

import java.util.*;

public class HarmonicBuffer {
    PriorityQueue<Harmonic> harmonicHierarchy;
    HashMap<Note, HashSet<Harmonic>> harmonicsTable;

    public HarmonicBuffer() {
        harmonicsTable = new HashMap<>();
    }

    void addHarmonicToHarmonicBuffer(double newHarmonicVolume, Note highestValueNote, Harmonic highestValueHarmonic, HashMap<Harmonic, Double> harmonicVolumeTable) {
        synchronized (harmonicVolumeTable) {
            harmonicVolumeTable.put(highestValueHarmonic, newHarmonicVolume);
        }
        synchronized (harmonicHierarchy) {
            harmonicHierarchy.add(highestValueHarmonic);
        }

        synchronized(harmonicsTable) {
            harmonicsTable.get(highestValueNote).add(highestValueHarmonic);
        }
    }

    double getHighestPriorityHarmonicVolume(int maxHarmonics, HashMap<Harmonic, Double> harmonicVolumeTable) {
        try {
            return harmonicVolumeTable.get(harmonicHierarchy.toArray(new Harmonic[harmonicHierarchy.size()])[maxHarmonics]);
        }
        catch(ArrayIndexOutOfBoundsException e){
            return 0;
        }
    }

    PriorityQueue<Harmonic> buildHarmonicHierarchy(HashMap<Note, Double> noteVolumeTable, HashMap<Harmonic, Double> harmonicVolumeTable) {
//            calculate harmonic volumes using note volumes
//            store volumes in a pair with the harmonic in a priorityqueue

        PriorityQueue<Harmonic> newHarmonicPriorityQueue = new PriorityQueue<>(
                (o1, o2) -> -harmonicVolumeTable.get(o1).compareTo(harmonicVolumeTable.get(o2)));
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

    HashMap<Harmonic, Double> rebuildHarmonicHierarchy(HashMap<Note, Double> noteVolumeTable) {
        synchronized(harmonicsTable) {
            harmonicsTable = updateHarmonicsTable(noteVolumeTable.keySet());
        }
        HashMap<Harmonic, Double> harmonicVolumeTable = calculateHarmonicVolumes(noteVolumeTable);
        harmonicHierarchy = buildHarmonicHierarchy(noteVolumeTable, harmonicVolumeTable);

        return harmonicVolumeTable;
    }

    private HashMap<Harmonic, Double> calculateHarmonicVolumes(HashMap<Note, Double> noteVolumeTable) {
        HashMap<Harmonic, Double> newHarmonicVolumeTable = new HashMap<>();
        synchronized (harmonicsTable) {
            for(Map.Entry<Note,Double> pair : new HashSet<>(noteVolumeTable.entrySet())){
                HashSet<Harmonic> harmonics = harmonicsTable.get(pair.getKey());
                for (Harmonic harmonic : harmonics) {
                    newHarmonicVolumeTable.put(harmonic, harmonic.getVolume(noteVolumeTable.get(pair.getKey())));
                }
            }
        }
        return newHarmonicVolumeTable;
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

    public LinkedList<Pair<Harmonic, Double>> getHarmonicHierarchy(HashMap<Harmonic, Double> harmonicVolumeTable) {
        LinkedList<Harmonic> harmonicHierarchy = new LinkedList<>(this.harmonicHierarchy);
        LinkedList<Pair<Harmonic, Double>> harmonicsWithVolumes = new LinkedList<>();
        for(Harmonic harmonic : harmonicHierarchy){
            harmonicsWithVolumes.addLast(new Pair(harmonic, harmonicVolumeTable.get(harmonic)));
        }
        return harmonicsWithVolumes;
    }
}