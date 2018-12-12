package spectrum.buckets;

import component.buffer.PipeCallable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BucketsAverager {

    public static PipeCallable<Buckets, Buckets> build(int averagingWidth) {
        return input -> {
            final double[] multipliers = new double[averagingWidth - 1];
            for (int i = 0; i < averagingWidth - 1; i++) {
                multipliers[i] = ((double) averagingWidth - (i + 1)) / averagingWidth;
            }

            Set<Buckets> bucketsToBeSummed = new HashSet<>();
            bucketsToBeSummed.add(input);

            Buckets hollowBuckets = getHollowBuckets(input);

            for (int i = 0; i < averagingWidth - 1; i++) {
                Buckets multipliedBuckets =
                        hollowBuckets
                                .multiply(multipliers[i]);

                int iAdjusted = i + 1;

                bucketsToBeSummed.add(
                        multipliedBuckets
                                .transpose(iAdjusted));

                bucketsToBeSummed.add(
                        multipliedBuckets
                                .transpose(-(iAdjusted)));
            }

            return Buckets.add(bucketsToBeSummed);
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