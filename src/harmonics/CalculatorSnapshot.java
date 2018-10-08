package harmonics;

import frequency.Frequency;

import java.util.*;

class CalculatorSnapshot {
    private final Map<Frequency, Double> volumeTable;
    private final PriorityQueue<Frequency> iteratorHierarchy;

    CalculatorSnapshot(Set<Frequency> liveFrequencies, Map<Frequency, MemoableIterator> iteratorTable, Map<Frequency, Double> volumeTable) {
        this.volumeTable = volumeTable;
        this.iteratorHierarchy = buildIteratorHierarchy(this.volumeTable, liveFrequencies, iteratorTable);
    }

    private PriorityQueue<Frequency> buildIteratorHierarchy(Map<Frequency, Double> volumeTable, Set<Frequency> liveFrequencies, Map<Frequency, MemoableIterator> iteratorTable) {
        PriorityQueue<Frequency> iteratorHierarchy = new PriorityQueue<>((o1, o2) -> -Double.compare(
                Harmonic.getHarmonicValue(volumeTable.get(o1), iteratorTable.get(o1).peek()),
                Harmonic.getHarmonicValue(volumeTable.get(o2), iteratorTable.get(o2).peek()))
        );
        iteratorHierarchy.addAll(liveFrequencies);

        return iteratorHierarchy;
    }

    Map<Frequency, Double> getVolumeTable() {
        return volumeTable;
    }

    PriorityQueue<Frequency> getIteratorHierarchy() {
        return iteratorHierarchy;
    }

}
