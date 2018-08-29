import java.util.*;

public class CalculatorSnapshot {
    private Map<Note, Double> volumeTable;
    private PriorityQueue<Note> iteratorHierarchy;

    public CalculatorSnapshot(Set<Note> liveNotes, Map<Note, MemoableIterator> iteratorTable, Map<Note, Double> volumeTable) {
        this.volumeTable = volumeTable;
        this.iteratorHierarchy = buildIteratorHierarchy(this.volumeTable, liveNotes, iteratorTable);
    }

    PriorityQueue<Note> buildIteratorHierarchy(Map<Note, Double> volumeTable, Set<Note> liveNotes, Map<Note, MemoableIterator> iteratorTable) {
        PriorityQueue<Note> iteratorHierarchy = new PriorityQueue<>((o1, o2) -> -Double.compare(
                Harmonic.getHarmonicValue(volumeTable.get(o1), iteratorTable.get(o1).peek()),
                Harmonic.getHarmonicValue(volumeTable.get(o2), iteratorTable.get(o2).peek()))
        );
        iteratorHierarchy.addAll(liveNotes);

        return iteratorHierarchy;
    }

    public Map<Note, Double> getVolumeTable() {
        return volumeTable;
    }

    public void setVolumeTable(HashMap<Note, Double> volumeTable) {
        this.volumeTable = volumeTable;
    }

    public PriorityQueue<Note> getIteratorHierarchy() {
        return iteratorHierarchy;
    }

    public void setIteratorHierarchy(PriorityQueue<Note> iteratorHierarchy) {
        this.iteratorHierarchy = iteratorHierarchy;
    }
}
