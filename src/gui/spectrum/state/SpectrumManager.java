package gui.spectrum.state;

import frequency.Frequency;
import gui.buckets.*;
import gui.spectrum.SpectrumWindow;
import harmonics.Harmonic;
import harmonics.HarmonicCalculator;
import main.BoundedBuffer;
import main.InputPort;
import main.OutputPort;
import notes.state.VolumeState;
import time.PerformanceTracker;
import time.TimeInNanoSeconds;
import time.TimeKeeper;

import java.util.*;

public class SpectrumManager implements Runnable {
    private final SpectrumWindow spectrumWindow;
    private final HarmonicCalculator harmonicCalculator;

    private BucketHistory bucketHistory;
    private SpectrumState spectrumState;

    private final InputPort<TimeInNanoSeconds> frameEndTimeInput;
    private final InputPort<VolumeState> volumeStateInput;
    private final OutputPort<Buckets> notesOutput;
    private final OutputPort<Buckets> harmonicsOutput;
    private Map<Frequency, Double> newPairs;
    private Set<Frequency> frequencies;

    public SpectrumManager(SpectrumWindow spectrumWindow, HarmonicCalculator harmonicCalculator, BoundedBuffer<TimeInNanoSeconds> frameEndTimeInputBuffer, BoundedBuffer<VolumeState> volumeStateBuffer, BoundedBuffer<Buckets> notesOutputBuffer, BoundedBuffer<Buckets> harmonicsOutputBuffer) {
        this.spectrumWindow = spectrumWindow;
        this.harmonicCalculator = harmonicCalculator;

        bucketHistory = new PrecalculatedBucketHistory(200);
        spectrumState = new SpectrumState(new Buckets(), new Buckets());

        frameEndTimeInput = new InputPort<>(frameEndTimeInputBuffer);
        volumeStateInput = new InputPort<>(volumeStateBuffer);
        notesOutput = new OutputPort<>(notesOutputBuffer);
        harmonicsOutput = new OutputPort<>(harmonicsOutputBuffer);

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
            TimeInNanoSeconds frameEndTime = frameEndTimeInput.consume();
            VolumeState volumeState = volumeStateInput.consume();

            TimeKeeper timeKeeper = PerformanceTracker.startTracking("create spectrum snapshot");
            Map<Frequency, Double> volumes = volumeState.volumes;
            Set<Frequency> liveFrequencies = volumes.keySet();
            Iterator<Map.Entry<Harmonic, Double>> harmonicHierarchyIterator = harmonicCalculator.getHarmonicHierarchyIterator(liveFrequencies, volumes);
            Buckets noteBuckets = toBuckets(liveFrequencies, volumes).precalculate();
            newPairs = new HashMap<>();
            frequencies = new HashSet<>();
            PerformanceTracker.stopTracking(timeKeeper);

            timeKeeper = PerformanceTracker.startTracking("build spectrum snapshot");
            int counter = 0;
            while (TimeInNanoSeconds.now().lessThan(frameEndTime)) {
                if (update(harmonicHierarchyIterator)) break;
                counter++;
            }
            System.out.println(counter);
            if(!TimeInNanoSeconds.now().lessThan(frameEndTime)){
                System.out.println("ran out of time");
            }
            PerformanceTracker.stopTracking(timeKeeper);

            spectrumState = finish(noteBuckets);

            timeKeeper = PerformanceTracker.startTracking("add to bucketHistory");
            bucketHistory = bucketHistory.addNewBuckets(spectrumState.harmonicsBuckets);
            PerformanceTracker.stopTracking(timeKeeper);

            timeKeeper = PerformanceTracker.startTracking("get timeAveragedBuckets");
            Buckets timeAveragedBuckets = bucketHistory.getTimeAveragedBuckets();
            PerformanceTracker.stopTracking(timeKeeper);

            notesOutput.produce(spectrumState.noteBuckets);
            harmonicsOutput.produce(timeAveragedBuckets);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //return true when harmonicHierarchy has been depleted.
    private boolean update(Iterator<Map.Entry<Harmonic, Double>> harmonicHierarchyIterator) {
        try {
            Map.Entry<Harmonic, Double> harmonicVolume = harmonicHierarchyIterator.next();
            Frequency frequency = harmonicVolume.getKey().getFrequency();

            if(spectrumWindow.inBounds(frequency)) {
                Double newValue;
                try {
                    newValue = newPairs.get(frequency) + harmonicVolume.getValue();
                } catch (NullPointerException e) {
                    frequencies.add(frequency);
                    newValue = harmonicVolume.getValue();
                }
                newPairs.put(frequency, newValue);
            }
        } catch (NoSuchElementException e) {
            return true;
        }
        return false;
    }

    private SpectrumState finish(Buckets noteBuckets) {
        TimeKeeper timeKeeper = PerformanceTracker.startTracking("harmonics toBuckets");
        Buckets newHarmonicsBuckets = toBuckets(frequencies, newPairs);
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("create SpectrumState");
        SpectrumState spectrumState = new SpectrumState(noteBuckets, newHarmonicsBuckets);
        PerformanceTracker.stopTracking(timeKeeper);

        return spectrumState;
    }

    private Buckets toBuckets(Set<Frequency> keys, Map<Frequency, Double> map){
        Set<Integer> indices = new HashSet<>();
        Map<Integer, Bucket> entries = new HashMap<>();

        for(Frequency frequency : keys){
            int x = spectrumWindow.getX(frequency);

            AtomicBucket bucket = new AtomicBucket(frequency, map.get(frequency));

            Buckets.fill(indices, entries, x, bucket);
        }
        return new Buckets(indices, entries).precalculate();
    }

}