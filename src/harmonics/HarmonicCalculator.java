package harmonics;

import frequency.Frequency;

import java.util.*;
import java.util.Map.Entry;

public class HarmonicCalculator {

    private CurrentTable<MemoableIterator> iteratorTable = new CurrentTable<>(MemoableIterator::new);
    private CurrentTable<Set<Harmonic>> harmonicsTable = new CurrentTable<>(HashSet::new);


    public Iterator<Entry<Harmonic, Double>> getHarmonicHierarchyIterator(Set<Frequency> liveFrequencies, Map<Frequency, Double> frequencyVolumeTable, int maxHarmonics) {
        CurrentTable<MemoableIterator> newIteratorTable = iteratorTable.getNewTable(liveFrequencies);
        CalculatorSnapshot calculatorSnapshot = new CalculatorSnapshot(liveFrequencies, newIteratorTable, frequencyVolumeTable);

        harmonicsTable = harmonicsTable.getNewTable(liveFrequencies);
        BufferSnapshot bufferSnapshot = new BufferSnapshot(liveFrequencies, harmonicsTable, calculatorSnapshot.getVolumeTable());

        addNewHarmonicsToBuffer(calculatorSnapshot, bufferSnapshot, maxHarmonics);

        iteratorTable = newIteratorTable;

        return bufferSnapshot.getHarmonicHierarchyAsList().iterator();
    }

    private void addNewHarmonicsToBuffer(CalculatorSnapshot calculatorSnapshot, BufferSnapshot bufferSnapshot, int maxHarmonics) {
        while(getNewHarmonicVolume(calculatorSnapshot) > bufferSnapshot.getHarmonicVolume(maxHarmonics)) {
            addNewHarmonic(calculatorSnapshot, bufferSnapshot);
        }
    }

    private double getNewHarmonicVolume(CalculatorSnapshot calculatorSnapshot) {
        try {
            Frequency highestValueFrequency = calculatorSnapshot.getIteratorHierarchy().peek();
            Fraction nextHarmonicAsFraction = iteratorTable.get(highestValueFrequency).peek();

            return Harmonic.getHarmonicValue(calculatorSnapshot.getVolumeTable().get(highestValueFrequency), nextHarmonicAsFraction);
        }
        catch(NullPointerException e){
            return 0.;
        }
    }

    private void addNewHarmonic(CalculatorSnapshot calculatorSnapshot, BufferSnapshot bufferSnapshot) {
        try {
            Frequency highestValueFrequency = calculatorSnapshot.getIteratorHierarchy().poll();
            Fraction nextHarmonicAsFraction = iteratorTable.get(highestValueFrequency).next();
            calculatorSnapshot.getIteratorHierarchy().add(highestValueFrequency);

            Harmonic highestValueHarmonic = new Harmonic(highestValueFrequency, nextHarmonicAsFraction);
            double newHarmonicVolume = highestValueHarmonic.getVolume(calculatorSnapshot.getVolumeTable().get(highestValueFrequency));

            harmonicsTable.get(highestValueFrequency).add(highestValueHarmonic);
            bufferSnapshot.addHarmonic(highestValueHarmonic, newHarmonicVolume);
        }
        catch(NullPointerException ignored){
        }
    }

}
