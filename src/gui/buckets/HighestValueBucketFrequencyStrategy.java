package gui.buckets;

import frequency.Frequency;

public class HighestValueBucketFrequencyStrategy implements BucketFrequencyStrategy {

    public static Frequency getFrequency(Bucket bucket) {
        Frequency highestFrequency = null;
        Double highestVolume = null;
        for(Frequency frequency : bucket.getFrequencies()){
            Double volume = bucket.getVolumes().get(frequency);
            try{
                if(volume > highestVolume){
                    highestFrequency = frequency;
                    highestVolume = volume;
                }
            }
            catch(NullPointerException e){
                highestFrequency = frequency;
                highestVolume = volume;
            }
        }
        return highestFrequency;
    }
}
