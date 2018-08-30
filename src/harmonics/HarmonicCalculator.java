package harmonics;

import javafx.util.Pair;

import java.util.*;

public class HarmonicCalculator {

    private CurrentTable<MemoableIterator> iteratorTable = new CurrentTable<>(() -> new MemoableIterator());
    private CurrentTable<Set<Harmonic>> harmonicsTable = new CurrentTable<>(() -> new HashSet<Harmonic>());


    public Iterator<Pair<Harmonic, Double>> getHarmonicHierarchyIterator(Set<Double> liveFrequencies, Map<Double, Double> frequencyVolumeTable, int maxHarmonics) {
        synchronized (iteratorTable) {
            CurrentTable<MemoableIterator> newIteratorTable = iteratorTable.getNewTable(liveFrequencies);
            CalculatorSnapshot calculatorSnapshot = new CalculatorSnapshot(liveFrequencies, newIteratorTable, frequencyVolumeTable);

            harmonicsTable = harmonicsTable.getNewTable(liveFrequencies);
            BufferSnapshot bufferSnapshot = new BufferSnapshot(harmonicsTable, calculatorSnapshot.getVolumeTable());

            addNewHarmonicsToBuffer(calculatorSnapshot, bufferSnapshot, maxHarmonics);

            iteratorTable = newIteratorTable;

            return bufferSnapshot.getHarmonicHierarchyAsList().iterator();
        }
    }

    private void addNewHarmonicsToBuffer(CalculatorSnapshot calculatorSnapshot, BufferSnapshot bufferSnapshot, int maxHarmonics) {
        while(getNewHarmonicVolume(calculatorSnapshot) > bufferSnapshot.getHarmonicVolume(maxHarmonics)) {
            addNewHarmonic(calculatorSnapshot, bufferSnapshot);
        }
    }

    private double getNewHarmonicVolume(CalculatorSnapshot calculatorSnapshot) {
        try {
            Double highestValueFrequency = calculatorSnapshot.getIteratorHierarchy().peek();
            Fraction nextHarmonicAsFraction = iteratorTable.get(highestValueFrequency).peek();

            return Harmonic.getHarmonicValue(calculatorSnapshot.getVolumeTable().get(highestValueFrequency), nextHarmonicAsFraction);
        }
        catch(NullPointerException e){
            return 0.;
        }
    }

    private void addNewHarmonic(CalculatorSnapshot calculatorSnapshot, BufferSnapshot bufferSnapshot) {
        try {
            Double highestValueFrequency = calculatorSnapshot.getIteratorHierarchy().poll();
            Fraction nextHarmonicAsFraction = iteratorTable.get(highestValueFrequency).next();
            calculatorSnapshot.getIteratorHierarchy().add(highestValueFrequency);

            Harmonic highestValueHarmonic = new Harmonic(highestValueFrequency, nextHarmonicAsFraction);
            double newHarmonicVolume = highestValueHarmonic.getVolume(calculatorSnapshot.getVolumeTable().get(highestValueFrequency));

            harmonicsTable.get(highestValueFrequency).add(highestValueHarmonic);
            bufferSnapshot.addHarmonic(highestValueHarmonic, newHarmonicVolume);
        }
        catch(NullPointerException e){
        }
    }

}
