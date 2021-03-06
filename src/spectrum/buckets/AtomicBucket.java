package spectrum.buckets;

import frequency.Frequency;

import java.util.*;

public class AtomicBucket implements Bucket {
    private final Frequency frequency;
    private final Double volume;

    public AtomicBucket(Frequency frequency, Double volume) {
        this.frequency = frequency;
        this.volume = volume;
    }

    @Override
    public Bucket add(Bucket bucket) {
        return new CompositeBucket<>(Arrays.asList(this, bucket));
    }

    @Override
    public Bucket multiply(double v) {
        return new AtomicBucket(frequency, volume * v);
    }

    @Override
    public Double getVolume() {
        return volume;
    }

    @Override
    public Set<Frequency> getFrequencies() {
        return new HashSet<>(Collections.singletonList(frequency));
    }

    @Override
    public Map<Frequency, Double> getVolumes() {
        Map<Frequency, Double> map = new HashMap<>();

        map.put(frequency, volume);

        return map;
    }
}
