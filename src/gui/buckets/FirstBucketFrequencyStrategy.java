package gui.buckets;

import frequency.Frequency;

public class FirstBucketFrequencyStrategy implements BucketFrequencyStrategy {

    public static Frequency getFrequency(Bucket bucket) {
        return bucket.getFrequencies().stream().findFirst().orElse(null);
    }

}
