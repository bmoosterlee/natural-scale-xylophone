package gui.spectrum.state;

import frequency.Frequency;
import gui.buckets.AtomicBucket;
import gui.buckets.Bucket;
import gui.buckets.Buckets;
import gui.spectrum.SpectrumWindow;
import main.BoundedBuffer;
import main.InputPort;
import main.OutputPort;
import main.Pulse;
import notes.state.VolumeState;
import time.PerformanceTracker;
import time.TimeKeeper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BucketBuilder implements Runnable {
    private final SpectrumWindow spectrumWindow;
    private final InputPort<Pulse> frameEndTimeInput;
    private final InputPort<VolumeState> volumeStateInput;
    private final OutputPort<Buckets> notesBucketsOutput;

    public BucketBuilder(SpectrumWindow spectrumWindow, BoundedBuffer<Pulse> frameEndTimeBuffer, BoundedBuffer<VolumeState> volumeStateBuffer, BoundedBuffer<Buckets> notesBucketsBuffer) {
        this.spectrumWindow = spectrumWindow;
        frameEndTimeInput = new InputPort<>(frameEndTimeBuffer);
        volumeStateInput = new InputPort<>(volumeStateBuffer);
        notesBucketsOutput = new OutputPort<>(notesBucketsBuffer);

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

    private void tick(){
        try {
            frameEndTimeInput.consume();
            VolumeState volumeState = volumeStateInput.consume();

            TimeKeeper timeKeeper = PerformanceTracker.startTracking("create spectrum snapshot");
            //todo remove this class as the middleman which creates buckets from the map
            Map<Frequency, Double> volumes = volumeState.volumes;
            Set<Frequency> liveFrequencies = volumes.keySet();
            Buckets noteBuckets = toBuckets(liveFrequencies, volumes).precalculate();
            PerformanceTracker.stopTracking(timeKeeper);

            notesBucketsOutput.produce(noteBuckets);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
