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
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;

public class HarmonicCalculator implements Runnable {

    private NewHarmonicsCalculator newHarmonicsCalculator;
    private MemoizedHighValueHarmonics memoizedHighValueHarmonics;

    private final int maxHarmonics;

    private final InputPort<VolumeState> volumeInput;
    private final OutputPort<Iterator<Map.Entry<Harmonic, Double>>> iteratorOutput;

    public HarmonicCalculator(int maxHarmonics, BoundedBuffer<VolumeState> inputBuffer, BoundedBuffer<Iterator<Map.Entry<Harmonic, Double>>> outputBuffer){
        this.maxHarmonics = maxHarmonics;

        newHarmonicsCalculator = new NewHarmonicsCalculator();
        memoizedHighValueHarmonics = new MemoizedHighValueHarmonics();

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
            VolumeState volumeState = volumeInput.consume();

            TimeKeeper timeKeeper = PerformanceTracker.startTracking("create harmonic hierarchy iterator");
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
        newHarmonicsCalculator = newHarmonicsCalculator.update(liveFrequencies, volumes);
        memoizedHighValueHarmonics = memoizedHighValueHarmonics.update(liveFrequencies, volumes);

        addNewHarmonicsToHarmonicsStorage();

        return memoizedHighValueHarmonics.getHarmonicHierarchyAsList().iterator();
    }

    private void addNewHarmonicsToHarmonicsStorage() {
        while(newHarmonicsCalculator.getNextHarmonicValue() > memoizedHighValueHarmonics.getHarmonicValue(maxHarmonics)) {
            addNewHarmonic();
        }
    }

    private void addNewHarmonic() {
        try {
            SimpleImmutableEntry<Frequency, SimpleImmutableEntry<Harmonic, Double>> highestValuePair = newHarmonicsCalculator.next();

            Frequency highestValueFrequency = highestValuePair.getKey();
            Harmonic highestValueHarmonic = highestValuePair.getValue().getKey();
            Double highestValue = highestValuePair.getValue().getValue();

            memoizedHighValueHarmonics.addHarmonic(highestValueFrequency, highestValueHarmonic, highestValue);
        }
        catch(NullPointerException ignored){
        }
    }
}
