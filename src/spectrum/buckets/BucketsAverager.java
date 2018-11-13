package spectrum.buckets;

import component.buffer.*;
import component.buffer.RunningPipeComponent;

import java.util.*;

public class BucketsAverager extends RunningPipeComponent<Buckets, Buckets> {

    public BucketsAverager(BoundedBuffer<Buckets> inputBuffer, BoundedBuffer<Buckets> outputBuffer, int averagingWidth) {
        super(inputBuffer, outputBuffer, build(averagingWidth));
    }

    public static CallableWithArguments<Buckets, Buckets> build(int averagingWidth){
        return new CallableWithArguments<>() {
            final OutputPort<Buckets> methodInput;
            final InputPort<Buckets> methodOutput;

            {
                int capacity = 10000;

                SimpleBuffer<Buckets> methodInputBuffer = new SimpleBuffer<>(capacity, "BucketsAverager - method input");
                methodInput = methodInputBuffer.createOutputPort();
                LinkedList<SimpleBuffer<Buckets>> methodInputBroadcast = new LinkedList<>(methodInputBuffer.broadcast(2));

                double[] multipliers = new double[averagingWidth - 1];
                for (int i = 0; i < averagingWidth - 1; i++) {
                    multipliers[i] = ((double) averagingWidth - (i+1)) / averagingWidth;
                }

                Set<BoundedBuffer<Buckets>> adderBuffers = new HashSet<>();
                adderBuffers.add(methodInputBroadcast.poll());

                LinkedList<SimpleBuffer<Buckets>> hollowBucketsBroadcast =
                    new LinkedList<>(
                        methodInputBroadcast.poll()
                        .performMethod(this::getHollowBuckets)
                        .broadcast(averagingWidth - 1));

                for (int i = 0; i < averagingWidth - 1; i++) {
                    int finalIMultiplier = i;

                    LinkedList<SimpleBuffer<Buckets>> multiplierBroadcast = new LinkedList<>(
                        hollowBucketsBroadcast
                        .poll()
                        .performMethod(input2 -> input2.multiply(multipliers[finalIMultiplier]))
                        .broadcast(2));

                    int finalI = i + 1;

                    adderBuffers.add(
                        multiplierBroadcast.poll()
                        .performMethod(input1 -> input1.transpose(finalI)));

                    adderBuffers.add(
                        multiplierBroadcast.poll()
                        .performMethod(input1 -> input1.transpose(-(finalI))));
                }

                SimpleBuffer<Buckets> adderOutputBuffer = new SimpleBuffer<>(capacity, "buckets averager - adder output");
                new Adder(adderBuffers, adderOutputBuffer);

                methodOutput = adderOutputBuffer.createInputPort();
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
                    methodInput.produce(buckets);

                    Buckets result = methodOutput.consume();

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