package gui;

import frequency.Frequency;

public class FirstBucketFrequencyStrategy implements BucketFrequencyStrategy {

    public static Frequency getFrequency(Bucket bucket) {
        for(Frequency frequency : bucket.frequencies){
            return frequency;
        }
        return null;
    }

}
