package gui.spectrum.state;

import frequency.Frequency;
import gui.buckets.AtomicBucket;
import gui.spectrum.SpectrumWindow;
import harmonics.Harmonic;
import harmonics.HarmonicCalculator;
import main.BoundedBuffer;
import main.InputPort;
import main.OutputPort;
import main.Pulse;
import notes.state.VolumeState;
import time.PerformanceTracker;
import time.TimeKeeper;

import java.util.*;

public class SpectrumManager implements Runnable {
    private final SpectrumWindow spectrumWindow;

    private final InputPort<Pulse> pulseInput;
    private final InputPort<Iterator<Map.Entry<Harmonic, Double>>> harmonicsInput;
    private final Map<Integer, OutputPort<AtomicBucket>> harmonicsOutput;

    public SpectrumManager(SpectrumWindow spectrumWindow, BoundedBuffer<Pulse> pulseBuffer, BoundedBuffer<Iterator<Map.Entry<Harmonic, Double>>> harmonicsBuffer, Map<Integer, BoundedBuffer<AtomicBucket>> bufferMap) {
        this.spectrumWindow = spectrumWindow;

        pulseInput = new InputPort<>(pulseBuffer);
        harmonicsInput = new InputPort<>(harmonicsBuffer);
        harmonicsOutput = new HashMap<>();
        for(Integer index : bufferMap.keySet()){
            harmonicsOutput.put(index, new OutputPort<>(bufferMap.get(index)));
        }

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
            Iterator<Map.Entry<Harmonic, Double>> harmonicHierarchyIterator = harmonicsInput.consume();

            TimeKeeper timeKeeper = PerformanceTracker.startTracking("build spectrum snapshot");
            while (pulseInput.isEmpty()) {
                if (update(harmonicHierarchyIterator)) break;
            }
            PerformanceTracker.stopTracking(timeKeeper);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //return true when harmonicHierarchy has been depleted.
    private boolean update(Iterator<Map.Entry<Harmonic, Double>> harmonicHierarchyIterator) {
        try {
            Map.Entry<Harmonic, Double> harmonicVolume = harmonicHierarchyIterator.next();
            Frequency frequency = harmonicVolume.getKey().getFrequency();

            AtomicBucket newBucket = new AtomicBucket(frequency, harmonicVolume.getValue());
            harmonicsOutput.get(spectrumWindow.getX(frequency)).produce(newBucket);

        } catch (NoSuchElementException e) {
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return true;
        } catch (NullPointerException ignored) {
            //Harmonic is out of bounds during the call to harmonicsOutput.get
        }
        return false;
    }

}