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

        inputPort = inputBuffer.createInputPort();
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
            final OutputPort<Buckets> hollowBucketsInput;
            final InputPort<Buckets> adderInputPort;

            {
                int capacity = 10000;
                int count = 0;

                double[] multipliers = new double[averagingWidth - 1];
                for (int i = 1; i < averagingWidth; i++) {
                    multipliers[i - 1] = ((double) averagingWidth - i) / averagingWidth;
                }

                BoundedBuffer<Buckets> hollowBucketsBuffer = new BoundedBuffer<>(capacity, "BucketsAverager" + String.valueOf(count));
                count++;
                hollowBucketsInput = hollowBucketsBuffer.createOutputPort();

                BoundedBuffer<Buckets>[] multiplierInputBuffers = hollowBucketsBuffer.broadcast(averagingWidth - 1).toArray(new BoundedBuffer[0]);

                BoundedBuffer<Buckets>[] multiplierOutputBuffers = new BoundedBuffer[averagingWidth - 1];
                for (int i = 0; i < averagingWidth - 1; i++) {
                    int finalI = i;
                    multiplierOutputBuffers[i] = multiplierInputBuffers[i].performMethod(input -> input.multiply(multipliers[finalI]));
                }

                BoundedBuffer<Buckets>[] multiplierOutputBuffersPositive = new BoundedBuffer[averagingWidth - 1];
                for (int i = 0; i < averagingWidth - 1; i++) {
                    multiplierOutputBuffersPositive[i] = new BoundedBuffer<>(capacity, "BucketsAverager" + String.valueOf(count));
                    count++;
                }
                BoundedBuffer<Buckets>[] multiplierOutputBuffersNegative = new BoundedBuffer[averagingWidth - 1];
                for (int i = 0; i < averagingWidth - 1; i++) {
                    multiplierOutputBuffersNegative[i] = new BoundedBuffer<>(capacity, "BucketsAverager" + String.valueOf(count));
                    count++;
                }
                for (int i = 0; i < averagingWidth - 1; i++) {
                    new Broadcast<>(multiplierOutputBuffers[i], Arrays.asList(multiplierOutputBuffersPositive[i], multiplierOutputBuffersNegative[i]));
                }

                Set<BoundedBuffer<Buckets>> transposedBuffers = new HashSet<>();
                for (int i = 0; i < averagingWidth - 1; i++) {
                    int finalI = i;
                    BoundedBuffer<Buckets> transposerOutputBuffer =
                            multiplierOutputBuffersPositive[i]
                                    .performMethod(input -> input.transpose(finalI + 1));

                    transposedBuffers.add(transposerOutputBuffer);
                }
                for (int i = 0; i < averagingWidth - 1; i++) {
                    int finalI = i;
                    BoundedBuffer<Buckets> transposerOutputBuffer =
                            multiplierOutputBuffersNegative[i]
                                    .performMethod(input -> input.transpose(-(finalI + 1)));

                    transposedBuffers.add(transposerOutputBuffer);
                }

                BoundedBuffer<Buckets> firstAdderBuffer = new BoundedBuffer<>(capacity, "BucketsAverager" + String.valueOf(count));
                count++;
                adderInput = firstAdderBuffer.createOutputPort();
                Set<BoundedBuffer<Buckets>> adderBuffers = new HashSet<>();
                adderBuffers.add(firstAdderBuffer);
                adderBuffers.addAll(transposedBuffers);
                BoundedBuffer<Buckets> adderOutputBuffer = new BoundedBuffer<>(capacity, "buckets averager - adder output");
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
                    Buckets hollowBuckets = getHollowBuckets(buckets);
                    PerformanceTracker.stopTracking(timeKeeper);

                    adderInput.produce(buckets);
                    hollowBucketsInput.produce(hollowBuckets);

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