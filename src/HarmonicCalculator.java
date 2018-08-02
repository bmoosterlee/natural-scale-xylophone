import javafx.util.Pair;

import java.util.*;

public class HarmonicCalculator {

    private CurrentTable<MemoableIterator> iteratorTable = new CurrentTable<>(() -> new MemoableIterator());
    private CurrentTable<HashSet<Harmonic>> harmonicsTable = new CurrentTable<>(() -> new HashSet<Harmonic>());


    public LinkedList<Pair<Harmonic, Double>> getHarmonicHierarchyAsList(HashSet<Note> liveNotes, int maxHarmonics, HashMap<Note, Double> volumeTable) {
        synchronized (iteratorTable) {
            CurrentTable<MemoableIterator> newIteratorTable = iteratorTable.getNewTable(liveNotes);
            CalculatorSnapshot calculatorSnapshot = new CalculatorSnapshot(liveNotes, newIteratorTable, volumeTable);

            harmonicsTable = harmonicsTable.getNewTable(liveNotes);
            BufferSnapshot bufferSnapshot = new BufferSnapshot(harmonicsTable, calculatorSnapshot.getVolumeTable());

            addNewHarmonicsToBuffer(calculatorSnapshot, bufferSnapshot, maxHarmonics);

            iteratorTable = newIteratorTable;

            return bufferSnapshot.getHarmonicHierarchyAsList();
        }
    }

    private void addNewHarmonicsToBuffer(CalculatorSnapshot calculatorSnapshot, BufferSnapshot bufferSnapshot, int maxHarmonics) {
        while(getNewHarmonicVolume(calculatorSnapshot) > bufferSnapshot.getHarmonicVolume(maxHarmonics)) {
            addNewHarmonic(calculatorSnapshot, bufferSnapshot);
        }
    }

    private double getNewHarmonicVolume(CalculatorSnapshot calculatorSnapshot) {
        try {
            Note highestValueNote = calculatorSnapshot.getIteratorHierarchy().peek();
            Fraction nextHarmonicAsFraction = iteratorTable.get(highestValueNote).peek();

            return Harmonic.getHarmonicValue(calculatorSnapshot.getVolumeTable().get(highestValueNote), nextHarmonicAsFraction);
        }
        catch(NullPointerException e){
            return 0.;
        }
    }

    private void addNewHarmonic(CalculatorSnapshot calculatorSnapshot, BufferSnapshot bufferSnapshot) {
        try {
            Note highestValueNote = calculatorSnapshot.getIteratorHierarchy().poll();
            Fraction nextHarmonicAsFraction = iteratorTable.get(highestValueNote).next();
            calculatorSnapshot.getIteratorHierarchy().add(highestValueNote);

            Harmonic highestValueHarmonic = new Harmonic(highestValueNote, nextHarmonicAsFraction);
            double newHarmonicVolume = highestValueHarmonic.getVolume(calculatorSnapshot.getVolumeTable().get(highestValueNote));

            harmonicsTable.get(highestValueNote).add(highestValueHarmonic);
            bufferSnapshot.addHarmonic(highestValueHarmonic, newHarmonicVolume);
        }
        catch(NullPointerException e){
        }
    }

}
