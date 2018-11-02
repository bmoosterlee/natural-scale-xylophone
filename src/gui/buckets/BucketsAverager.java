package gui.buckets;

import main.BoundedBuffer;
import main.Broadcast;
import main.InputPort;
import main.OutputPort;
import time.PerformanceTracker;
import time.TimeKeeper;

import java.util.*;

public class BucketsAverager implements Runnable {
    private final int averagingWidth;
    private final double[] multipliers;

    private final InputPort<Buckets> bucketsInput;
    private final OutputPort<Buckets> adderInput;
    private final OutputPort<Buckets> multiplierInput;

    public BucketsAverager(int averagingWidth, BoundedBuffer<Buckets> inputBuffer, BoundedBuffer<Buckets> outputBuffer) {
        this.averagingWidth = averagingWidth;

        multipliers = new double[this.averagingWidth-1];
        for (int i = 1; i < this.averagingWidth; i++) {
            multipliers[i-1] = ((double) this.averagingWidth - i) / this.averagingWidth;
        }

        BoundedBuffer<Buckets> hollowBucketsBuffer = new BoundedBuffer<>(1);
        multiplierInput = new OutputPort<>(hollowBucketsBuffer);

        BoundedBuffer<Buckets>[] multiplierInputBuffers = new BoundedBuffer[this.averagingWidth-1];
        BoundedBuffer<Buckets>[] multiplierOutputBuffers = new BoundedBuffer[this.averagingWidth-1];

        for(int i = 0; i < this.averagingWidth-1; i++) {
            BoundedBuffer<Buckets> multiplierInputBuffer = new BoundedBuffer<>(1);
            BoundedBuffer<Buckets> multiplierOutputBuffer = new BoundedBuffer<>(1);
            new Multiplier(multiplierInputBuffer, multiplierOutputBuffer, multipliers[i]);

            multiplierInputBuffers[i] = multiplierInputBuffer;
            multiplierOutputBuffers[i] = multiplierOutputBuffer;
        }

        new Broadcast<>(hollowBucketsBuffer, new HashSet<>(Arrays.asList(multiplierInputBuffers)));

        BoundedBuffer<Buckets>[] multiplierOutputBuffersPositive = new BoundedBuffer[this.averagingWidth-1];
        for(int i = 0; i < this.averagingWidth-1; i++) {
            multiplierOutputBuffersPositive[i] = new BoundedBuffer<>(1);
        }

        BoundedBuffer<Buckets>[] multiplierOutputBuffersNegative = new BoundedBuffer[this.averagingWidth-1];
        for(int i = 0; i < this.averagingWidth-1; i++) {
            multiplierOutputBuffersNegative[i] = new BoundedBuffer<>(1);
        }

        for(int i = 0; i < this.averagingWidth-1; i++) {
            new Broadcast(multiplierOutputBuffers[i], new HashSet(Arrays.asList(multiplierOutputBuffersPositive[i], multiplierOutputBuffersNegative[i])));
        }

        OutputPort<Buckets>[] transposerInputs = new OutputPort[(averagingWidth-1)*2];

        Set<BoundedBuffer<Buckets>> adderBuffers = new HashSet<>();
        BoundedBuffer<Buckets> firstAdderBuffer = new BoundedBuffer<>(1);
        adderInput = new OutputPort<>(firstAdderBuffer);
        adderBuffers.add(firstAdderBuffer);

        for(int i = 0; i< averagingWidth-1; i++) {
            BoundedBuffer<Buckets> transposerInputBuffer = multiplierOutputBuffersPositive[i];
            BoundedBuffer<Buckets> transposerOutputBuffer = new BoundedBuffer<>(1);

            new Transposer(transposerInputBuffer, transposerOutputBuffer, (i+1));
            transposerInputs[i] = new OutputPort<>(transposerInputBuffer);

            adderBuffers.add(transposerOutputBuffer);
        }

        for(int i = 0; i< averagingWidth-1; i++) {
            BoundedBuffer<Buckets> transposerInputBuffer = multiplierOutputBuffersNegative[i];
            BoundedBuffer<Buckets> transposerOutputBuffer = new BoundedBuffer<>(1);

            new Transposer(transposerInputBuffer, transposerOutputBuffer, -(i+1));
            transposerInputs[averagingWidth-1+i] = new OutputPort<>(transposerInputBuffer);

            adderBuffers.add(transposerOutputBuffer);
        }

        new Adder(adderBuffers, outputBuffer);

        bucketsInput = new InputPort<>(inputBuffer);

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

            averageBuckets(newBuckets);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Buckets getHollowBuckets(Buckets buckets) {
        Set<Integer> indices = buckets.getIndices();
        Map<Integer, Bucket> voidEntries = new HashMap<>();

        for(Integer x : indices){
            Double volume = buckets.getValue(x).getVolume();
            voidEntries.put(x, new AtomicBucket(volume));
        }

        return new Buckets(indices, voidEntries);
    }

    private void averageBuckets(Buckets buckets) {
        try {
            TimeKeeper timeKeeper = PerformanceTracker.startTracking("average buckets");
            Buckets voidBuckets = getHollowBuckets(buckets);
            PerformanceTracker.stopTracking(timeKeeper);

            adderInput.produce(buckets);

            multiplierInput.produce(voidBuckets);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}