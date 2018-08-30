package harmonics;

import javafx.util.Pair;

import java.util.*;

public class BufferSnapshot {
    private final Map<Harmonic, Double> harmonicVolumeTable;
    private final PriorityQueue<Harmonic> harmonicHierarchy;

    public BufferSnapshot(Map<Double, Set<Harmonic>> harmonicsTable, Map<Double, Double> noteVolumeTable) {
        harmonicVolumeTable = calculateHarmonicVolumes(harmonicsTable, noteVolumeTable);
        harmonicHierarchy = buildHarmonicHierarchy(harmonicVolumeTable);
    }

    private PriorityQueue<Harmonic> buildHarmonicHierarchy(Map<Harmonic, Double> harmonicVolumeTable) {
//            calculate harmonic volumes using note volumes
//            store volumes in a pair with the harmonic in a priorityqueue

        PriorityQueue<Harmonic> newHarmonicPriorityQueue = new PriorityQueue<>(
                (o1, o2) -> -harmonicVolumeTable.get(o1).compareTo(harmonicVolumeTable.get(o2)));
        for(Harmonic harmonic : harmonicVolumeTable.keySet()){
            newHarmonicPriorityQueue.add(harmonic);
        }
        return newHarmonicPriorityQueue;
    }

    private Map<Harmonic, Double> calculateHarmonicVolumes(
            Map<Double, Set<Harmonic>> harmonicsTable, Map<Double, Double> frequencyVolumeTable) {
        Map<Harmonic, Double> newHarmonicVolumeTable = new HashMap<>();
        synchronized (harmonicsTable) {
            for(Map.Entry<Double, Double> pair : new HashSet<>(frequencyVolumeTable.entrySet())){
                Set<Harmonic> harmonics = harmonicsTable.get(pair.getKey());
                for (Harmonic harmonic : harmonics) {
                    newHarmonicVolumeTable.put(harmonic, harmonic.getVolume(pair.getValue()));
                }
            }
        }
        return newHarmonicVolumeTable;
    }

    public Map<Harmonic, Double> getHarmonicVolumeTable() {
        return harmonicVolumeTable;
    }

    public PriorityQueue<Harmonic> getHarmonicHierarchy() {
        return harmonicHierarchy;
    }

    public void addHarmonic(Harmonic highestValueHarmonic, double newHarmonicVolume) {
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
            Map<Harmonic, Double> harmonicVolumeTable = getHarmonicVolumeTable();
            PriorityQueue<Harmonic> harmonicHierarchy = getHarmonicHierarchy();
            return harmonicVolumeTable.get(harmonicHierarchy.toArray(new Harmonic[harmonicHierarchy.size()])[index]);
        }
        catch(ArrayIndexOutOfBoundsException e){
            return 0;
        }
    }
}
