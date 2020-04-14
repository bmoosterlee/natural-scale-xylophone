package spectrum.buckets;

import frequency.Frequency;

import java.util.*;

public class HollowBucket implements Bucket {
    private final Double volume;

    public HollowBucket(double volume) {
        this.volume = volume;
    }

    @Override
    public Bucket add(Bucket bucket) {
        return new CompositeBucket<>(Arrays.asList(this, bucket));
    }

    @Override
    public Bucket multiply(double v) {
        return new HollowBucket(volume * v);
    }

    @Override
    public Double getVolume() {
        return volume;
    }

    @Override
    public Set<Frequency> getFrequencies() {
        return new HashSet<>(Collections.emptySet());
    }

    @Override
    public Map<Frequency, Double> getVolumes() {
        return Collections.emptyMap();
    }

}
