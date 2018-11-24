package spectrum.buckets;

import component.buffer.*;

import java.util.*;

public class BucketsAverager extends MethodPipeComponent<Buckets, Buckets> {

    public BucketsAverager(SimpleBuffer<Buckets> inputBuffer, BoundedBuffer<Buckets> outputBuffer, int averagingWidth) {
        super(inputBuffer, outputBuffer, build(averagingWidth));
    }

    public static PipeCallable<Buckets, Buckets> build(int averagingWidth){
        return MethodPipeComponent.toMethod(buildPipe(averagingWidth));
    }

    public static PipeCallable<BoundedBuffer<Buckets>, BoundedBuffer<Buckets>> buildPipe(int averagingWidth) {
        return inputBuffer -> {

            LinkedList<SimpleBuffer<Buckets>> methodInputBroadcast =
                    new LinkedList<>(
                            inputBuffer
                                    .broadcast(2, "buckets averager - broadcast"));

            final double[] multipliers = new double[averagingWidth - 1];
            for (int i = 0; i < averagingWidth - 1; i++) {
                multipliers[i] = ((double) averagingWidth - (i + 1)) / averagingWidth;
            }

            Set<BoundedBuffer<Buckets>> adderBuffers = new HashSet<>();
            adderBuffers.add(methodInputBroadcast.poll());

            LinkedList<SimpleBuffer<Buckets>> hollowBucketsBroadcast =
                    new LinkedList<>(
                            methodInputBroadcast.poll()
                                    .performMethod(BucketsAverager::getHollowBuckets, "hollow buckets - output")
                                    .broadcast(averagingWidth - 1, "hollow buckets - broadcast"));

            for (int i = 0; i < averagingWidth - 1; i++) {
                int finalIMultiplier = i;

                LinkedList<SimpleBuffer<Buckets>> multiplierBroadcast =
                        new LinkedList<>(
                                hollowBucketsBroadcast.poll()
                                        .performMethod(input2 -> input2.multiply(multipliers[finalIMultiplier]), "buckets averager multiplier - output")
                                        .broadcast(2, "buckets averager multiplier - broadcast"));

                int finalI = i + 1;

                adderBuffers.add(
                        multiplierBroadcast.poll()
                                .performMethod(input1 -> input1.transpose(finalI), "buckets averager transpose positive"));

                adderBuffers.add(
                        multiplierBroadcast.poll()
                                .performMethod(input1 -> input1.transpose(-(finalI)), "buckets averager transpose negative"));
            }

            return Adder.build(adderBuffers);
        };
    }

    private static Buckets getHollowBuckets(Buckets buckets) {
        Set<Integer> indices = buckets.getIndices();
        Map<Integer, Bucket> voidEntries = new HashMap<>();

        for (Integer x : indices) {
            Double volume = buckets.getValue(x).getVolume();
            voidEntries.put(x, new AtomicBucket(volume));
        }

        return new Buckets(indices, voidEntries);
    }
}