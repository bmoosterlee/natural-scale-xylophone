package spectrum.buckets;

import frequency.Frequency;

import java.util.*;

//todo create a frequency and volume class.
//todo use this class internally in Bucket.
//todo create a composite version of this class where one frequency has one volume.
//todo let a bucket be a collection of these instances.
//todo minimize constructing new object when a reference could do too.

public class AtomicBucket implements Bucket {
    private final Frequency frequency;
    private final Double volume;

    public AtomicBucket(Frequency frequency, Double volume) {
        this.frequency = frequency;
        this.volume = volume;
    }

    AtomicBucket(double volume) {
        this.frequency = null;
        this.volume = volume;
    }

    @Override
    public Bucket add(Bucket bucket) {
        Bucket newBucket = new CompositeBucket<>(Arrays.asList(this, bucket));

        return newBucket;
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