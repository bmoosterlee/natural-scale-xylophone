package gui.buckets;

import main.BoundedBuffer;
import main.InputPort;
import main.OutputPort;
import time.PerformanceTracker;
import time.TimeKeeper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BucketsAverager implements Runnable {
    private final int averagingWidth;
    private final double[] multipliers;

    private final InputPort<Buckets> bucketsInput;
    private final OutputPort<Buckets> averagedBucketsOutput;

    public BucketsAverager(int averagingWidth, BoundedBuffer<Buckets> inputBuffer, BoundedBuffer<Buckets> outputBuffer) {
        this.averagingWidth = averagingWidth;

        double[] multipliers = new double[averagingWidth];
        for (int i = 1; i < averagingWidth; i++) {
            multipliers[i] = ((double) averagingWidth - i) / averagingWidth;
        }
        this.multipliers = multipliers;

        bucketsInput = new InputPort<>(inputBuffer);
        averagedBucketsOutput = new OutputPort<>(outputBuffer);

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
            Buckets newBuckets = bucketsInput.consume();

            Buckets averagedBuckets = averageBuckets(newBuckets);

            averagedBucketsOutput.produce(averagedBuckets);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Buckets averageBuckets(Buckets buckets) {
        TimeKeeper timeKeeper = PerformanceTracker.startTracking("average buckets setup");
        Set<Integer> newIndices = new HashSet<>();
        Map<Integer, Bucket> newEntries = new HashMap<>();
        PerformanceTracker.stopTracking(timeKeeper);

        for(Integer x : buckets.getIndices()){
            timeKeeper = PerformanceTracker.startTracking("average buckets get volume");
            Double volume = buckets.getValue(x).getVolume();
            PerformanceTracker.stopTracking(timeKeeper);

            for(int i = 1; i< averagingWidth; i++) {
                timeKeeper = PerformanceTracker.startTracking("average buckets multiply bucket");
                AtomicBucket residueBucket = new AtomicBucket(volume * multipliers[i]);
                PerformanceTracker.stopTracking(timeKeeper);

                {
                    int residueIndex = x - i;

                    Buckets.fill(newIndices, newEntries, residueIndex, residueBucket);
                }
                {
                    int residueIndex = x + i;

                    Buckets.fill(newIndices, newEntries, residueIndex, residueBucket);
                }
            }
        }

        timeKeeper = PerformanceTracker.startTracking("average buckets construct new buckets");
        Buckets newBuckets = new Buckets(newIndices, newEntries);
        PerformanceTracker.stopTracking(timeKeeper);

        return buckets.add(newBuckets);
    }
}