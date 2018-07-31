import javafx.util.Pair;

import java.util.*;

public class HarmonicCalculator {

    private HashMap<Note, MemoableIterator> iteratorTable;
    HashMap<Note, HashSet<Harmonic>> harmonicsTable;

    public HarmonicCalculator(){
        iteratorTable = new HashMap<>();
        harmonicsTable = new HashMap<>();
    }

    public LinkedList<Pair<Harmonic, Double>> getHarmonicHierarchyAsList(long currentSampleCount, HashSet<Note> liveNotes, int maxHarmonics) {
        synchronized (iteratorTable) {
            HashMap<Note, MemoableIterator> newIteratorTable = getNewIteratorTable(iteratorTable, liveNotes);
            CalculatorSnapshot calculatorSnapshot = new CalculatorSnapshot(currentSampleCount, liveNotes, newIteratorTable);

            harmonicsTable = getNewHarmonicsTable(harmonicsTable, liveNotes);
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

    HashMap<Note, HashSet<Harmonic>> getNewHarmonicsTable(HashMap<Note, HashSet<Harmonic>> harmonicsTable, Set<Note> liveNotes) {
        HashMap<Note, HashSet<Harmonic>> newHarmonicsTable = new HashMap<>();
        for (Note note : liveNotes) {
            if(harmonicsTable.containsKey(note)) {
                newHarmonicsTable.put(note, harmonicsTable.get(note));
            }
            else{
                newHarmonicsTable.put(note, new HashSet<>());
            }
        }
        return newHarmonicsTable;
    }

    private HashMap<Note, MemoableIterator> getNewIteratorTable(HashMap<Note, MemoableIterator> iteratorTable, HashSet<Note> liveNotes) {
        HashMap<Note, MemoableIterator> newIteratorTable = new HashMap<>();
        for (Note note : liveNotes) {
            if(iteratorTable.containsKey(note)) {
                newIteratorTable.put(note, iteratorTable.get(note));
            }
            else{
                newIteratorTable.put(note, new MemoableIterator());
            }
        }
        return newIteratorTable;
    }

}
