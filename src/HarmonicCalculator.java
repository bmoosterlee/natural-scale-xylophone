import javafx.util.Pair;

import java.util.*;

public class HarmonicCalculator {
    
    private final HarmonicBuffer harmonicBuffer = new HarmonicBuffer();

    private HashMap<Note, MemoableIterator> iteratorTable;

    public HarmonicCalculator(){
        iteratorTable = new HashMap<>();
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

            harmonicBuffer.addHarmonic(highestValueNote, highestValueHarmonic, newHarmonicVolume, bufferSnapshot);
        }
        catch(NullPointerException e){
        }
    }

    public LinkedList<Pair<Harmonic, Double>> getHarmonicHierarchyAsList(long currentSampleCount, HashSet<Note> liveNotes, int maxHarmonics) {
        iteratorTable = getNewIteratorTable(liveNotes);

        CalculatorSnapshot calculatorSnapshot = new CalculatorSnapshot(currentSampleCount, liveNotes, iteratorTable);
        BufferSnapshot bufferSnapshot = harmonicBuffer.getBufferSnapshot(calculatorSnapshot.getVolumeTable());

        addNewHarmonicsToBuffer(calculatorSnapshot, bufferSnapshot, maxHarmonics);

        return bufferSnapshot.getHarmonicHierarchyAsList();
    }

    private HashMap<Note, MemoableIterator> getNewIteratorTable(HashSet<Note> liveNotes) {
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
