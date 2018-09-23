package harmonics;

import frequency.Frequency;

import java.util.*;

public class CalculatorSnapshot {
    //todo change fields to final
    private Map<Frequency, Double> volumeTable;
    private PriorityQueue<Frequency> iteratorHierarchy;

    public CalculatorSnapshot(Set<Frequency> liveFrequencies, Map<Frequency, MemoableIterator> iteratorTable, Map<Frequency, Double> volumeTable) {
        this.volumeTable = volumeTable;
        this.iteratorHierarchy = buildIteratorHierarchy(this.volumeTable, liveFrequencies, iteratorTable);
    }

    PriorityQueue<Frequency> buildIteratorHierarchy(Map<Frequency, Double> volumeTable, Set<Frequency> liveFrequencies, Map<Frequency, MemoableIterator> iteratorTable) {
        PriorityQueue<Frequency> iteratorHierarchy = new PriorityQueue<>((o1, o2) -> -Double.compare(
                Harmonic.getHarmonicValue(volumeTable.get(o1), iteratorTable.get(o1).peek()),
                Harmonic.getHarmonicValue(volumeTable.get(o2), iteratorTable.get(o2).peek()))
        );
        iteratorHierarchy.addAll(liveFrequencies);

        return iteratorHierarchy;
    }

    public Map<Frequency, Double> getVolumeTable() {
        return volumeTable;
    }

    public PriorityQueue<Frequency> getIteratorHierarchy() {
        return iteratorHierarchy;
    }

}
