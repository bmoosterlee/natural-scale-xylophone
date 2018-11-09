package spectrum;

import frequency.Frequency;
import spectrum.buckets.AtomicBucket;
import spectrum.buckets.Bucket;
import spectrum.buckets.Buckets;
import component.BoundedBuffer;
import component.InputPort;
import component.OutputPort;
import mixer.state.VolumeState;
import time.PerformanceTracker;
import time.TimeKeeper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class VolumeStateToBuckets implements Runnable {
    private final SpectrumWindow spectrumWindow;

    private final InputPort<VolumeState> volumeStateInput;
    private final OutputPort<Buckets> notesBucketsOutput;

    public VolumeStateToBuckets(SpectrumWindow spectrumWindow, BoundedBuffer<VolumeState> volumeStateBuffer, BoundedBuffer<Buckets> notesBucketsBuffer) {
        this.spectrumWindow = spectrumWindow;

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
            VolumeState volumeState = volumeStateInput.consume();

            TimeKeeper timeKeeper = PerformanceTracker.startTracking("create spectrum snapshot");
            //todo remove this class as the middleman which creates buckets from the map
            Map<Frequency, Double> volumes = volumeState.volumes;
            Set<Frequency> liveFrequencies = volumes.keySet();
            Buckets noteBuckets = toBuckets(liveFrequencies, volumes);
            PerformanceTracker.stopTracking(timeKeeper);

            notesBucketsOutput.produce(noteBuckets);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Buckets toBuckets(Set<Frequency> keys, Map<Frequency, Double> volumes){
        Set<Integer> indices = new HashSet<>();
        Map<Integer, Bucket> entries = new HashMap<>();

        for(Frequency frequency : keys){
            int x = spectrumWindow.getX(frequency);

            indices.add(x);
            entries.put(x, new AtomicBucket(frequency, volumes.get(frequency)));
        }

        return new Buckets(indices, entries);
    }
}