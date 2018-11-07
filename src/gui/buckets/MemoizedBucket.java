package gui.buckets;

import frequency.Frequency;
import time.PerformanceTracker;
import time.TimeKeeper;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

public class MemoizedBucket implements Bucket {
    private final Bucket bucket;

    private Double volume;
    private Set<Frequency> frequencies;
    private Map<Frequency, Double> volumes;

    MemoizedBucket(Bucket bucket){
        this.bucket = bucket;
    }

    @Override
    public Bucket add(Bucket bucket) {
        CompositeBucket<Bucket> compositeBucket = new CompositeBucket<>(Arrays.asList(this, bucket));
        return compositeBucket;
    }

    @Override
    public Bucket multiply(double v) {
        return bucket.multiply(v);
    }

    @Override
    public Double getVolume() {
        if(volume==null){
            volume = bucket.getVolume();
        }
        return volume;
    }

    @Override
    public Set<Frequency> getFrequencies() {
        if(frequencies==null){
            frequencies = bucket.getFrequencies();
        }
        return frequencies;
    }

    @Override
    public Map<Frequency, Double> getVolumes() {
        if(volumes==null){
            volumes = bucket.getVolumes();
        }
        return volumes;
    }
}
