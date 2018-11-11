package spectrum.buckets;

import component.*;
import time.PerformanceTracker;
import time.TimeKeeper;

import java.util.*;

public class BucketsAverager extends Tickable {
    private final CallableWithArguments<Buckets, Buckets> averager;

    private final InputPort<Buckets> inputPort;
    private final OutputPort<Buckets> outputPort;

    public BucketsAverager(int averagingWidth, BoundedBuffer<Buckets> inputBuffer, BoundedBuffer<Buckets> outputBuffer) {
        averager = average(averagingWidth);

        inputPort = new InputPort<>(inputBuffer);
        outputPort = outputBuffer.createOutputPort();

        start();
    }

    protected void tick(){
        try {
            Buckets newBuckets = inputPort.consume();
            Buckets result = averager.call(newBuckets);
            outputPort.produce(result);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static CallableWithArguments<Buckets, Buckets> average(int averagingWidth){
        return new CallableWithArguments<>() {
            final OutputPort<Buckets> adderInput;
            final OutputPort<Buckets> multiplierInput;
            final InputPort<Buckets> adderInputPort;

            {
                int capacity = 10000;
                int count = 0;

                double[] multipliers = new double[averagingWidth - 1];
                for (int i = 1; i < averagingWidth; i++) {
                    multipliers[i - 1] = ((double) averagingWidth - i) / averagingWidth;
                }

                SimpleBuffer<Buckets> hollowBucketsBuffer = new SimpleBuffer<>(capacity, "BucketsAverager" + String.valueOf(count));
                count++;
                multiplierInput = new OutputPort<>(hollowBucketsBuffer);

                BoundedBuffer<Buckets>[] multiplierInputBuffers = new BoundedBuffer[averagingWidth - 1];
                BoundedBuffer<Buckets>[] multiplierOutputBuffers = new BoundedBuffer[averagingWidth - 1];

                for (int i = 0; i < averagingWidth - 1; i++) {
                    BoundedBuffer<Buckets> multiplierInputBuffer = new SimpleBuffer<>(capacity, "BucketsAverager" + String.valueOf(count));
                    count++;
                    SimpleBuffer<Buckets> multiplierOutputBuffer = new SimpleBuffer<>(capacity, "BucketsAverager" + String.valueOf(count));
                    count++;
                    new Multiplier(multiplierInputBuffer, multiplierOutputBuffer, multipliers[i]);

                    multiplierInputBuffers[i] = multiplierInputBuffer;
                    multiplierOutputBuffers[i] = multiplierOutputBuffer;
                }

                new Broadcast<>(hollowBucketsBuffer, new HashSet<>(Arrays.asList(multiplierInputBuffers)));

                SimpleBuffer<Buckets>[] multiplierOutputBuffersPositive = new SimpleBuffer[averagingWidth - 1];
                for (int i = 0; i < averagingWidth - 1; i++) {
                    multiplierOutputBuffersPositive[i] = new SimpleBuffer<>(capacity, "BucketsAverager" + String.valueOf(count));
                    count++;
                }

                SimpleBuffer<Buckets>[] multiplierOutputBuffersNegative = new SimpleBuffer[averagingWidth - 1];
                for (int i = 0; i < averagingWidth - 1; i++) {
                    multiplierOutputBuffersNegative[i] = new SimpleBuffer<>(capacity, "BucketsAverager" + String.valueOf(count));
                    count++;
                }

                for (int i = 0; i < averagingWidth - 1; i++) {
                    new Broadcast(multiplierOutputBuffers[i], new HashSet(Arrays.asList(multiplierOutputBuffersPositive[i], multiplierOutputBuffersNegative[i])));
                }

                OutputPort<Buckets>[] transposerInputs = new OutputPort[(averagingWidth - 1) * 2];

                Set<BoundedBuffer<Buckets>> adderBuffers = new HashSet<>();
                SimpleBuffer<Buckets> firstAdderBuffer = new SimpleBuffer<>(capacity, "BucketsAverager" + String.valueOf(count));
                count++;
                adderInput = new OutputPort<>(firstAdderBuffer);
                adderBuffers.add(firstAdderBuffer);

                for (int i = 0; i < averagingWidth - 1; i++) {
                    SimpleBuffer<Buckets> transposerInputBuffer = multiplierOutputBuffersPositive[i];
                    SimpleBuffer<Buckets> transposerOutputBuffer = new SimpleBuffer<>(capacity, "BucketsAverager" + String.valueOf(count));
                    count++;

                    new Transposer(transposerInputBuffer, transposerOutputBuffer, (i + 1));
                    transposerInputs[i] = new OutputPort<>(transposerInputBuffer);

                    adderBuffers.add(transposerOutputBuffer);
                }

                for (int i = 0; i < averagingWidth - 1; i++) {
                    SimpleBuffer<Buckets> transposerInputBuffer = multiplierOutputBuffersNegative[i];
                    SimpleBuffer<Buckets> transposerOutputBuffer = new SimpleBuffer<>(capacity, "BucketsAverager" + String.valueOf(count));
                    count++;

                    new Transposer(transposerInputBuffer, transposerOutputBuffer, -(i + 1));
                    transposerInputs[averagingWidth - 1 + i] = new OutputPort<>(transposerInputBuffer);

                    adderBuffers.add(transposerOutputBuffer);
                }

                SimpleBuffer<Buckets> adderOutputBuffer = new SimpleBuffer<>(capacity, "buckets averager - adder output");
                new Adder(adderBuffers, adderOutputBuffer);

                adderInputPort = adderOutputBuffer.createInputPort();
            }

            private Buckets getHollowBuckets(Buckets buckets) {
                Set<Integer> indices = buckets.getIndices();
                Map<Integer, Bucket> voidEntries = new HashMap<>();

                for (Integer x : indices) {
                    Double volume = buckets.getValue(x).getVolume();
                    voidEntries.put(x, new AtomicBucket(volume));
                }

                return new Buckets(indices, voidEntries);
            }

            private Buckets averageBuckets(Buckets buckets) {
                try {
                    TimeKeeper timeKeeper = PerformanceTracker.startTracking("average buckets");
                    Buckets voidBuckets = getHollowBuckets(buckets);
                    PerformanceTracker.stopTracking(timeKeeper);

                    adderInput.produce(buckets);
                    multiplierInput.produce(voidBuckets);

                    Buckets result = adderInputPort.consume();
                    return result;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public Buckets call(Buckets input) {
                return averageBuckets(input);
            }
        };
    }
}