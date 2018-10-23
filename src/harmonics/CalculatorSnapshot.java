package harmonics;

import frequency.Frequency;

import java.util.*;

class CalculatorSnapshot {
    private final PriorityQueue<Frequency> iteratorHierarchy;

    CalculatorSnapshot(Set<Frequency> liveFrequencies, Map<Frequency, MemoableIterator> iteratorTable, Map<Frequency, Double> volumeTable) {
        PriorityQueue<Frequency> iteratorHierarchy1 = new PriorityQueue<>((o1, o2) -> -Double.compare(
                Harmonic.getHarmonicValue(volumeTable.get(o1), iteratorTable.get(o1).peek()),
                Harmonic.getHarmonicValue(volumeTable.get(o2), iteratorTable.get(o2).peek()))
        );
        iteratorHierarchy1.addAll(liveFrequencies);

        this.iteratorHierarchy = iteratorHierarchy1;
    }

    PriorityQueue<Frequency> getIteratorHierarchy() {
        return iteratorHierarchy;
    }

    Frequency poll() {
        Frequency highestValueFrequency = iteratorHierarchy.poll();
        iteratorHierarchy.add(highestValueFrequency);
        return highestValueFrequency;
    }
}
