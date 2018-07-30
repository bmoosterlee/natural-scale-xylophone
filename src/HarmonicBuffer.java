import javafx.util.Pair;

import java.util.*;

public class HarmonicBuffer {
    HashMap<Note, HashSet<Harmonic>> harmonicsTable;

    public HarmonicBuffer() {
        harmonicsTable = new HashMap<>();
    }

    void addHarmonicToHarmonicBuffer(double newHarmonicVolume, Note highestValueNote, Harmonic highestValueHarmonic, HashMap<Harmonic, Double> harmonicVolumeTable, PriorityQueue<Harmonic> harmonicHierarchy) {
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

    double getHighestPriorityHarmonicVolume(int maxHarmonics, HashMap<Harmonic, Double> harmonicVolumeTable, PriorityQueue<Harmonic> harmonicHierarchy) {
        try {
            return harmonicVolumeTable.get(harmonicHierarchy.toArray(new Harmonic[harmonicHierarchy.size()])[maxHarmonics]);
        }
        catch(ArrayIndexOutOfBoundsException e){
            return 0;
        }
    }

    PriorityQueue<Harmonic> buildHarmonicHierarchy(HashMap<Harmonic, Double> harmonicVolumeTable) {
//            calculate harmonic volumes using note volumes
//            store volumes in a pair with the harmonic in a priorityqueue

        PriorityQueue<Harmonic> newHarmonicPriorityQueue = new PriorityQueue<>(
                (o1, o2) -> -harmonicVolumeTable.get(o1).compareTo(harmonicVolumeTable.get(o2)));
        for(Harmonic harmonic : harmonicVolumeTable.keySet()){
            newHarmonicPriorityQueue.add(harmonic);
        }
        return newHarmonicPriorityQueue;
    }

    Pair<HashMap<Harmonic, Double>, PriorityQueue<Harmonic>> rebuildHarmonicHierarchy(HashMap<Note, Double> noteVolumeTable) {
        synchronized(harmonicsTable) {
            harmonicsTable = updateHarmonicsTable(noteVolumeTable.keySet());
        }
        HashMap<Harmonic, Double> harmonicVolumeTable = calculateHarmonicVolumes(noteVolumeTable);
        PriorityQueue<Harmonic> harmonicHierarchy = buildHarmonicHierarchy(harmonicVolumeTable);

        return new Pair(harmonicVolumeTable, harmonicHierarchy);
    }

    private HashMap<Harmonic, Double> calculateHarmonicVolumes(HashMap<Note, Double> noteVolumeTable) {
        HashMap<Harmonic, Double> newHarmonicVolumeTable = new HashMap<>();
        synchronized (harmonicsTable) {
            for(Map.Entry<Note,Double> pair : new HashSet<>(noteVolumeTable.entrySet())){
                HashSet<Harmonic> harmonics = harmonicsTable.get(pair.getKey());
                for (Harmonic harmonic : harmonics) {
                    newHarmonicVolumeTable.put(harmonic, harmonic.getVolume(pair.getValue()));
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

    public LinkedList<Pair<Harmonic, Double>> getHarmonicHierarchy(HashMap<Harmonic, Double> harmonicVolumeTable, PriorityQueue<Harmonic> harmonicHierarchy) {
        LinkedList<Harmonic> harmonicHierarchyCopy = new LinkedList<>(harmonicHierarchy);
        LinkedList<Pair<Harmonic, Double>> harmonicsWithVolumes = new LinkedList<>();
        for(Harmonic harmonic : harmonicHierarchyCopy){
            harmonicsWithVolumes.addLast(new Pair(harmonic, harmonicVolumeTable.get(harmonic)));
        }
        return harmonicsWithVolumes;
    }
}