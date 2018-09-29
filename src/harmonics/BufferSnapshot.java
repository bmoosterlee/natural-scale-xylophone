package harmonics;

import frequency.Frequency;

import java.util.*;
import java.util.Map.Entry;

public class BufferSnapshot {
    private final Set<Frequency> liveFrequencies;
    private final Map<Harmonic, Double> harmonicVolumeTable;
    private final PriorityQueue<Harmonic> harmonicHierarchy;

    public BufferSnapshot(Set<Frequency> liveFrequencies, Map<Frequency, Set<Harmonic>> harmonicsTable, Map<Frequency, Double> volumeTable) {
        this.liveFrequencies = liveFrequencies;
        harmonicVolumeTable = calculateHarmonicVolumes(harmonicsTable, volumeTable);
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
            Map<Frequency, Set<Harmonic>> harmonicsTable, Map<Frequency, Double> frequencyVolumeTable) {
        Map<Harmonic, Double> newHarmonicVolumeTable = new HashMap<>();
        synchronized (harmonicsTable) {
            for(Frequency frequency : liveFrequencies){
                Set<Harmonic> harmonics = harmonicsTable.get(frequency);
                for (Harmonic harmonic : harmonics) {
                    newHarmonicVolumeTable.put(harmonic, harmonic.getVolume(frequencyVolumeTable.get(frequency)));
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

    public LinkedList<Entry<Harmonic, Double>> getHarmonicHierarchyAsList() {
        LinkedList<Harmonic> harmonicHierarchyCopy = new LinkedList<>(getHarmonicHierarchy());
        LinkedList<Entry<Harmonic, Double>> harmonicsWithVolumes = new LinkedList<>();
        for(Harmonic harmonic : harmonicHierarchyCopy){
            harmonicsWithVolumes.addLast(new AbstractMap.SimpleImmutableEntry<>(harmonic, getHarmonicVolumeTable().get(harmonic)));
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
