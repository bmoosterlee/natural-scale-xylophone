package harmonics;

import java.util.*;

public class CalculatorSnapshot {
    //todo change fields to final
    private Map<Double, Double> volumeTable;
    private PriorityQueue<Double> iteratorHierarchy;

    public CalculatorSnapshot(Set<Double> liveNotes, Map<Double, MemoableIterator> iteratorTable, Map<Double, Double> volumeTable) {
        this.volumeTable = volumeTable;
        this.iteratorHierarchy = buildIteratorHierarchy(this.volumeTable, liveNotes, iteratorTable);
    }

    PriorityQueue<Double> buildIteratorHierarchy(Map<Double, Double> volumeTable, Set<Double> liveFrequencies, Map<Double, MemoableIterator> iteratorTable) {
        PriorityQueue<Double> iteratorHierarchy = new PriorityQueue<>((o1, o2) -> -Double.compare(
                Harmonic.getHarmonicValue(volumeTable.get(o1), iteratorTable.get(o1).peek()),
                Harmonic.getHarmonicValue(volumeTable.get(o2), iteratorTable.get(o2).peek()))
        );
        iteratorHierarchy.addAll(liveFrequencies);

        return iteratorHierarchy;
    }

    public Map<Double, Double> getVolumeTable() {
        return volumeTable;
    }

    public void setVolumeTable(HashMap<Double, Double> volumeTable) {
        this.volumeTable = volumeTable;
    }

    public PriorityQueue<Double> getIteratorHierarchy() {
        return iteratorHierarchy;
    }

    public void setIteratorHierarchy(PriorityQueue<Double> iteratorHierarchy) {
        this.iteratorHierarchy = iteratorHierarchy;
    }
}
