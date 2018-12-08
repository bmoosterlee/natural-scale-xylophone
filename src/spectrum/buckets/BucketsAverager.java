package spectrum.buckets;

import component.Collator;
import component.buffer.*;

import java.util.*;

public class BucketsAverager {

    public static <A extends Packet<Buckets>, B extends Packet<Buckets>> PipeCallable<BoundedBuffer<Buckets, A>, BoundedBuffer<Buckets, B>> buildPipe(int averagingWidth) {
        return inputBuffer -> {

            LinkedList<SimpleBuffer<Buckets, A>> methodInputBroadcast =
                    new LinkedList<>(
                            inputBuffer
                                    .broadcast(2, "buckets averager - broadcast"));

            final double[] multipliers = new double[averagingWidth - 1];
            for (int i = 0; i < averagingWidth - 1; i++) {
                multipliers[i] = ((double) averagingWidth - (i + 1)) / averagingWidth;
            }

            Set<BoundedBuffer<Buckets, A>> adderBuffers = new HashSet<>();
            adderBuffers.add(methodInputBroadcast.poll());

            LinkedList<SimpleBuffer<Buckets, A>> hollowBucketsBroadcast =
                    new LinkedList<>(
                            methodInputBroadcast.poll()
                                    .<Buckets, A>performMethod(((PipeCallable<Buckets, Buckets>) BucketsAverager::getHollowBuckets).toSequential(), "hollow buckets - output")
                                    .broadcast(averagingWidth - 1, "hollow buckets - broadcast"));

            for (int i = 0; i < averagingWidth - 1; i++) {
                int finalIMultiplier = i;

                LinkedList<SimpleBuffer<Buckets, A>> multiplierBroadcast =
                        new LinkedList<>(
                                hollowBucketsBroadcast.poll()
                                        .<Buckets, A>performMethod(((PipeCallable<Buckets, Buckets>) input2 -> input2.multiply(multipliers[finalIMultiplier])).toSequential(), "buckets averager multiplier - output")
                                        .broadcast(2, "buckets averager multiplier - broadcast"));

                int finalI = i + 1;

                adderBuffers.add(
                        multiplierBroadcast.poll()
                                .performMethod(((PipeCallable<Buckets, Buckets>) input1 -> input1.transpose(finalI)).toSequential(), "buckets averager transpose positive"));

                adderBuffers.add(
                        multiplierBroadcast.poll()
                                .performMethod(((PipeCallable<Buckets, Buckets>) input1 -> input1.transpose(-(finalI))).toSequential(), "buckets averager transpose negative"));
            }

            return Collator.collate(new ArrayList<>(adderBuffers)).performMethod(((PipeCallable<List<Buckets>, Buckets>) Buckets::add).toSequential(), "buckets averager - add buckets");
        };
    }

    private static Buckets getHollowBuckets(Buckets buckets) {
        Set<Integer> indices = buckets.getIndices();
        Map<Integer, Bucket> voidEntries = new HashMap<>();

        for (Integer x : indices) {
            Double volume = buckets.getValue(x).getVolume();
            voidEntries.put(x, new HollowBucket(volume));
        }

        return new Buckets(indices, voidEntries);
    }
}