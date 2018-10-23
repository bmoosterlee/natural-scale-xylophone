package harmonics;

import frequency.Frequency;
import main.BoundedBuffer;
import main.InputPort;
import main.OutputPort;
import main.Pulse;
import notes.state.VolumeState;
import time.PerformanceTracker;
import time.TimeKeeper;

import java.util.*;
import java.util.Map.Entry;

public class HarmonicCalculator implements Runnable {

    private CurrentTable<MemoableIterator> iteratorTable = new CurrentTable<>(MemoableIterator::new);

    private HighValueHarmonicsStorage highValueHarmonicsStorage;

    private final int maxHarmonics;

    private final InputPort<Pulse> pulseInput;
    private final InputPort<VolumeState> volumeInput;
    private final OutputPort<Iterator<Map.Entry<Harmonic, Double>>> iteratorOutput;

    public HarmonicCalculator(int maxHarmonics, BoundedBuffer<Pulse> pulseBuffer, BoundedBuffer<VolumeState> inputBuffer, BoundedBuffer<Iterator<Map.Entry<Harmonic, Double>>> outputBuffer){
        this.maxHarmonics = maxHarmonics;

        highValueHarmonicsStorage = new HighValueHarmonicsStorage();

        pulseInput = new InputPort<>(pulseBuffer);
        volumeInput = new InputPort<>(inputBuffer);
        iteratorOutput = new OutputPort<>(outputBuffer);

        start();
    }

    private void start() {
        new Thread(this).start();
    }

    @Override
    public void run() {
        while(true){
            tick();
        }
    }

    private void tick() {
        try {
            pulseInput.consume();
            VolumeState volumeState = volumeInput.consume();

            TimeKeeper timeKeeper = PerformanceTracker.startTracking("create spectrum snapshot");
            Map<Frequency, Double> volumes = volumeState.volumes;
            Set<Frequency> liveFrequencies = volumes.keySet();
            Iterator<Map.Entry<Harmonic, Double>> harmonicHierarchyIterator = getHarmonicHierarchyIterator(liveFrequencies, volumes);
            PerformanceTracker.stopTracking(timeKeeper);

            iteratorOutput.produce(harmonicHierarchyIterator);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private Iterator<Entry<Harmonic, Double>> getHarmonicHierarchyIterator(Set<Frequency> liveFrequencies, Map<Frequency, Double> volumes) {
        CurrentTable<MemoableIterator> newIteratorTable = iteratorTable.getNewTable(liveFrequencies);
        CalculatorSnapshot calculatorSnapshot = new CalculatorSnapshot(liveFrequencies, newIteratorTable, volumes);

        highValueHarmonicsStorage = highValueHarmonicsStorage.update(liveFrequencies, volumes);

        addNewHarmonicsToHarmonicsStorage(calculatorSnapshot, volumes, maxHarmonics);

        iteratorTable = newIteratorTable;

        return highValueHarmonicsStorage.getHarmonicHierarchyAsList().iterator();
    }

    private void addNewHarmonicsToHarmonicsStorage(CalculatorSnapshot calculatorSnapshot, Map<Frequency, Double> volumes, int maxHarmonics) {
        while(getNewHarmonicVolume(calculatorSnapshot, volumes) > highValueHarmonicsStorage.getHarmonicVolume(maxHarmonics)) {
            addNewHarmonic(calculatorSnapshot, volumes);
        }
    }

    private double getNewHarmonicVolume(CalculatorSnapshot calculatorSnapshot, Map<Frequency, Double> volumes) {
        try {
            Frequency highestValueFrequency = calculatorSnapshot.getIteratorHierarchy().peek();
            Fraction nextHarmonicAsFraction = iteratorTable.get(highestValueFrequency).peek();

            return Harmonic.getHarmonicValue(volumes.get(highestValueFrequency), nextHarmonicAsFraction);
        }
        catch(NullPointerException e){
            return 0.;
        }
    }

    private void addNewHarmonic(CalculatorSnapshot calculatorSnapshot, Map<Frequency, Double> volumes) {
        try {
            Frequency highestValueFrequency = calculatorSnapshot.poll();
            Fraction highestValueHarmonicAsFraction = iteratorTable.get(highestValueFrequency).next();

            Harmonic highestValueHarmonic = new Harmonic(highestValueFrequency, highestValueHarmonicAsFraction);
            double newHarmonicVolume = highestValueHarmonic.getVolume(volumes.get(highestValueFrequency));

            highValueHarmonicsStorage.addHarmonic(highestValueFrequency, highestValueHarmonic, newHarmonicVolume);
        }
        catch(NullPointerException ignored){
        }
    }
}
