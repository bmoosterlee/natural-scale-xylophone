package harmonics;

import frequency.Frequency;

import java.util.*;
import java.util.Map.Entry;

class HighValueHarmonicsStorage {
    private final CurrentTable<Set<Harmonic>> harmonicsTable;

    private final Map<Harmonic, Double> harmonicVolumeTable;
    private final PriorityQueue<Harmonic> harmonicHierarchy;

    HighValueHarmonicsStorage(){
        this(new CurrentTable<>(HashSet::new), new HashMap<>(), new PriorityQueue<>());
    }

    private HighValueHarmonicsStorage(CurrentTable<Set<Harmonic>> harmonicsTable, Map<Harmonic, Double> harmonicVolumeTable, PriorityQueue<Harmonic> harmonicHierarchy) {
        this.harmonicsTable = harmonicsTable;
        this.harmonicVolumeTable = harmonicVolumeTable;
        this.harmonicHierarchy = harmonicHierarchy;
    }

    HighValueHarmonicsStorage update(Set<Frequency> liveFrequencies, Map<Frequency, Double> volumeTable) {
        CurrentTable<Set<Harmonic>> newHarmonicsTable = harmonicsTable.getNewTable(liveFrequencies);
        Map<Harmonic, Double> newHarmonicVolumeTable = calculateHarmonicVolumes(liveFrequencies, newHarmonicsTable, volumeTable);
        PriorityQueue<Harmonic> newHarmonicHierarchy = buildHarmonicHierarchy(newHarmonicVolumeTable);

        return new HighValueHarmonicsStorage(newHarmonicsTable, newHarmonicVolumeTable, newHarmonicHierarchy);
    }

    private PriorityQueue<Harmonic> buildHarmonicHierarchy(Map<Harmonic, Double> harmonicVolumeTable) {
        PriorityQueue<Harmonic> newHarmonicPriorityQueue = new PriorityQueue<>(
                (o1, o2) -> -harmonicVolumeTable.get(o1).compareTo(harmonicVolumeTable.get(o2)));
        newHarmonicPriorityQueue.addAll(harmonicVolumeTable.keySet());
        return newHarmonicPriorityQueue;
    }

    private Map<Harmonic, Double> calculateHarmonicVolumes(
            Set<Frequency> liveFrequencies, Map<Frequency, Set<Harmonic>> harmonicsTable, Map<Frequency, Double> frequencyVolumeTable) {
        Map<Harmonic, Double> newHarmonicVolumeTable = new HashMap<>();

        for(Frequency frequency : liveFrequencies){
            Set<Harmonic> harmonics = harmonicsTable.get(frequency);
            for (Harmonic harmonic : harmonics) {
                newHarmonicVolumeTable.put(harmonic, harmonic.getVolume(frequencyVolumeTable.get(frequency)));
            }
        }
        return newHarmonicVolumeTable;
    }

    void addHarmonic(Frequency highestValueFrequency, Harmonic highestValueHarmonic, double newHarmonicVolume) {
        harmonicsTable.get(highestValueFrequency).add(highestValueHarmonic);

        synchronized (harmonicVolumeTable) {
            harmonicVolumeTable.put(highestValueHarmonic, newHarmonicVolume);
        }
        synchronized (harmonicHierarchy) {
            harmonicHierarchy.add(highestValueHarmonic);
        }
    }

    LinkedList<Entry<Harmonic, Double>> getHarmonicHierarchyAsList() {
        LinkedList<Harmonic> harmonicHierarchyCopy = new LinkedList<>(harmonicHierarchy);
        LinkedList<Entry<Harmonic, Double>> harmonicsWithVolumes = new LinkedList<>();
        for(Harmonic harmonic : harmonicHierarchyCopy){
            harmonicsWithVolumes.addLast(new AbstractMap.SimpleImmutableEntry<>(harmonic, harmonicVolumeTable.get(harmonic)));
        }
        return harmonicsWithVolumes;
    }

    double getHarmonicVolume(int maxHarmonics) {
        try {
            return harmonicVolumeTable.get(harmonicHierarchy.toArray(new Harmonic[0])[maxHarmonics]);
        }
        catch(ArrayIndexOutOfBoundsException e){
            return 0;
        }
    }
}
