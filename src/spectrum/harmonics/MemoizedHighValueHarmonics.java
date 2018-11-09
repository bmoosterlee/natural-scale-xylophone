package spectrum.harmonics;

import frequency.Frequency;

import java.util.*;
import java.util.Map.Entry;

class MemoizedHighValueHarmonics {
    private final CurrentTable<Set<Harmonic>> harmonicsTable;
    private final Map<Harmonic, Double> volumes;
    private final PriorityQueue<Harmonic> hierarchy;

    MemoizedHighValueHarmonics(){
        this(new CurrentTable<>(HashSet::new), new HashMap<>(), new PriorityQueue<>());
    }

    private MemoizedHighValueHarmonics(CurrentTable<Set<Harmonic>> harmonicsTable, Map<Harmonic, Double> volumes, PriorityQueue<Harmonic> hierarchy) {
        this.harmonicsTable = harmonicsTable;
        this.volumes = volumes;
        this.hierarchy = hierarchy;
    }

    MemoizedHighValueHarmonics update(Set<Frequency> liveFrequencies, Map<Frequency, Double> volumes) {
        CurrentTable<Set<Harmonic>> newHarmonicsTable = harmonicsTable.update(liveFrequencies);
        Map<Harmonic, Double> newVolumes = calculateHarmonicVolumes(liveFrequencies, newHarmonicsTable, volumes);
        PriorityQueue<Harmonic> newHierarchy = buildHierarchy(newVolumes);

        return new MemoizedHighValueHarmonics(newHarmonicsTable, newVolumes, newHierarchy);
    }

    private Map<Harmonic, Double> calculateHarmonicVolumes(
            Set<Frequency> liveFrequencies, Map<Frequency, Set<Harmonic>> harmonicsTable, Map<Frequency, Double> frequencyVolumes) {
        Map<Harmonic, Double> newHarmonicVolumeTable = new HashMap<>();

        for(Frequency frequency : liveFrequencies){
            Set<Harmonic> harmonics = harmonicsTable.get(frequency);
            for (Harmonic harmonic : harmonics) {
                newHarmonicVolumeTable.put(harmonic, harmonic.getVolume(frequencyVolumes.get(frequency)));
            }
        }
        return newHarmonicVolumeTable;
    }

    private PriorityQueue<Harmonic> buildHierarchy(Map<Harmonic, Double> harmonicVolumes) {
        PriorityQueue<Harmonic> newHierarchy = new PriorityQueue<>(
                (o1, o2) -> -harmonicVolumes.get(o1).compareTo(harmonicVolumes.get(o2)));
        newHierarchy.addAll(harmonicVolumes.keySet());
        return newHierarchy;
    }

    void addHarmonic(Frequency highestValueFrequency, Harmonic highestValueHarmonic, double newHarmonicVolume) {
        harmonicsTable.get(highestValueFrequency).add(highestValueHarmonic);
        volumes.put(highestValueHarmonic, newHarmonicVolume);
        hierarchy.add(highestValueHarmonic);
    }

    List<Entry<Harmonic, Double>> getHarmonicHierarchyAsList() {
        LinkedList<Entry<Harmonic, Double>> harmonicsWithVolumes = new LinkedList<>();
        for(Harmonic harmonic : hierarchy){
            harmonicsWithVolumes.addLast(new AbstractMap.SimpleImmutableEntry<>(harmonic, volumes.get(harmonic)));
        }
        return harmonicsWithVolumes;
    }

    double getHarmonicValue(int maxHarmonics) {
        try {
            return volumes.get(hierarchy.toArray(new Harmonic[0])[maxHarmonics]);
        }
        catch(ArrayIndexOutOfBoundsException e){
            return 0;
        }
    }
}
