package gui;

import frequency.Frequency;

import java.util.*;

public class CompositeBucket<T extends Bucket> implements Bucket {
    private Set<T> buckets;

    CompositeBucket(Collection<T> buckets) {
        this.buckets = new HashSet<>(buckets);
    }

    @Override
    public Bucket add(Bucket bucket) {
        Set<Bucket> newBuckets = new HashSet<>(buckets);

        newBuckets.add(bucket);

        return new CompositeBucket<>(newBuckets);
    }

    public Bucket add(CompositeBucket<Bucket> bucket) {
        Set<Bucket> newBuckets = new HashSet<>(buckets);

        newBuckets.addAll(bucket.buckets);

        return new CompositeBucket<>(newBuckets);
    }

    @Override
    public Bucket multiply(double v) {
        Set<Bucket> newBuckets = new HashSet<>();

        for(Bucket bucket : buckets){
            newBuckets.add(bucket.multiply(v));
        }

        return new CompositeBucket<>(newBuckets);
    }

    @Override
    public Double getVolume() {
        Double volume = 0.;

        for(Bucket bucket : buckets){
            volume += bucket.getVolume();
        }

        return volume;
    }

    @Override
    public Set<Frequency> getFrequencies() {
        Set<Frequency> newFrequencies = new HashSet<>();

        for(Bucket bucket : buckets){
            newFrequencies.addAll(bucket.getFrequencies());
        }

        return newFrequencies;
    }

    @Override
    public Map<Frequency, Double> getVolumes() {
        Map<Frequency, Double> newVolumes = new HashMap<>();

        for(Bucket bucket : buckets){
            for(Frequency frequency : bucket.getFrequencies()) {

                Double newVolume;
                Double otherVolume = bucket.getVolumes().get(frequency);

                try {
                    newVolume = newVolumes.get(frequency) + otherVolume;
                }
                catch(NullPointerException e){
                    newVolume = otherVolume;
                }

                newVolumes.put(frequency, newVolume);
            }
        }

        return newVolumes;
    }
}
