import javafx.util.Pair;

import java.util.*;

public class BufferSnapshot {
    private final HashMap<Harmonic, Double> harmonicVolumeTable;
    private final PriorityQueue<Harmonic> harmonicHierarchy;

    public BufferSnapshot(HashMap<Note, HashSet<Harmonic>> harmonicsTable, HashMap<Note, Double> noteVolumeTable) {
        harmonicVolumeTable = calculateHarmonicVolumes(harmonicsTable, noteVolumeTable);
        harmonicHierarchy = buildHarmonicHierarchy(harmonicVolumeTable);
    }

    private PriorityQueue<Harmonic> buildHarmonicHierarchy(HashMap<Harmonic, Double> harmonicVolumeTable) {
//            calculate harmonic volumes using note volumes
//            store volumes in a pair with the harmonic in a priorityqueue

        PriorityQueue<Harmonic> newHarmonicPriorityQueue = new PriorityQueue<>(
                (o1, o2) -> -harmonicVolumeTable.get(o1).compareTo(harmonicVolumeTable.get(o2)));
        for(Harmonic harmonic : harmonicVolumeTable.keySet()){
            newHarmonicPriorityQueue.add(harmonic);
        }
        return newHarmonicPriorityQueue;
    }

    private HashMap<Harmonic, Double> calculateHarmonicVolumes(
            HashMap<Note, HashSet<Harmonic>> harmonicsTable, HashMap<Note, Double> noteVolumeTable) {
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

    public HashMap<Harmonic, Double> getHarmonicVolumeTable() {
        return harmonicVolumeTable;
    }

    public PriorityQueue<Harmonic> getHarmonicHierarchy() {
        return harmonicHierarchy;
    }

    public void addHarmonic(double newHarmonicVolume, Harmonic highestValueHarmonic) {
        synchronized (getHarmonicVolumeTable()) {
            getHarmonicVolumeTable().put(highestValueHarmonic, newHarmonicVolume);
        }
        synchronized (getHarmonicHierarchy()) {
            getHarmonicHierarchy().add(highestValueHarmonic);
        }
    }

    public LinkedList<Pair<Harmonic, Double>> getHarmonicHierarchyAsList() {
        LinkedList<Harmonic> harmonicHierarchyCopy = new LinkedList<>(getHarmonicHierarchy());
        LinkedList<Pair<Harmonic, Double>> harmonicsWithVolumes = new LinkedList<>();
        for(Harmonic harmonic : harmonicHierarchyCopy){
            harmonicsWithVolumes.addLast(new Pair(harmonic, getHarmonicVolumeTable().get(harmonic)));
        }
        return harmonicsWithVolumes;
    }

    double getHarmonicVolume(int index) {
        try {
            HashMap<Harmonic, Double> harmonicVolumeTable = getHarmonicVolumeTable();
            PriorityQueue<Harmonic> harmonicHierarchy = getHarmonicHierarchy();
            return harmonicVolumeTable.get(harmonicHierarchy.toArray(new Harmonic[harmonicHierarchy.size()])[index]);
        }
        catch(ArrayIndexOutOfBoundsException e){
            return 0;
        }
    }
}
