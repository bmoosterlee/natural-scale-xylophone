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

        multipliers = new double[this.averagingWidth];
        for (int i = 1; i < this.averagingWidth; i++) {
            multipliers[i] = ((double) this.averagingWidth - i) / this.averagingWidth;
        }

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
        Buckets voidBuckets = getVoidBuckets(buckets);

        TimeKeeper timeKeeper = PerformanceTracker.startTracking("average buckets multiply and transpose buckets");
        Set<Buckets> bucketsCollection = new HashSet<>();
        bucketsCollection.add(buckets);

        for(int i = 1; i< averagingWidth; i++) {
            Buckets multipliedBuckets = voidBuckets.multiply(multipliers[i]);
            bucketsCollection.add(multipliedBuckets.transpose(-i));
            bucketsCollection.add(multipliedBuckets.transpose(i));
        }
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("average buckets construct new buckets");
        Buckets newBuckets = Buckets.add(bucketsCollection);
        PerformanceTracker.stopTracking(timeKeeper);

        return newBuckets;
    }

    private Buckets getVoidBuckets(Buckets buckets) {
        TimeKeeper timeKeeper = PerformanceTracker.startTracking("average buckets setup");
        Set<Integer> indices = buckets.getIndices();
        Map<Integer, Bucket> voidEntries = new HashMap<>();

        for(Integer x : indices){
            Double volume = buckets.getValue(x).getVolume();
            voidEntries.put(x, new AtomicBucket(volume));
        }
        Buckets voidBuckets = new Buckets(indices, voidEntries);
        PerformanceTracker.stopTracking(timeKeeper);
        return voidBuckets;
    }
}