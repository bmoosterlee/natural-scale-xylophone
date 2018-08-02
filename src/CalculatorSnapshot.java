import java.util.*;

public class CalculatorSnapshot {
    private HashMap<Note, Double> volumeTable;
    private PriorityQueue<Note> iteratorHierarchy;

    public CalculatorSnapshot(Set<Note> liveNotes, HashMap<Note, MemoableIterator> iteratorTable, HashMap<Note, Double> volumeTable) {
        this.volumeTable = volumeTable;
        this.iteratorHierarchy = buildIteratorHierarchy(this.volumeTable, liveNotes, iteratorTable);
    }

    PriorityQueue<Note> buildIteratorHierarchy(HashMap<Note, Double> volumeTable, Set<Note> liveNotes, HashMap<Note, MemoableIterator> iteratorTable) {
        PriorityQueue<Note> iteratorHierarchy = new PriorityQueue<>((o1, o2) -> -Double.compare(
                Harmonic.getHarmonicValue(volumeTable.get(o1), iteratorTable.get(o1).peek()),
                Harmonic.getHarmonicValue(volumeTable.get(o2), iteratorTable.get(o2).peek()))
        );
        iteratorHierarchy.addAll(liveNotes);

        return iteratorHierarchy;
    }

    public HashMap<Note, Double> getVolumeTable() {
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
