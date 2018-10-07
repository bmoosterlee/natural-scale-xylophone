package gui.buckets;

import frequency.Frequency;

public class FirstBucketFrequencyStrategy implements BucketFrequencyStrategy {

    public static Frequency getFrequency(Bucket bucket) {
        for(Frequency frequency : bucket.getFrequencies()){
            return frequency;
        }
        return null;
    }

}
