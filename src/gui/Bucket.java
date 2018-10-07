package gui;

import frequency.Frequency;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Bucket {
    public final Double volume;

    final Set<Frequency> frequencies;
    final Map<Frequency, Double> volumes;

    public Bucket(Frequency frequency, Double volume) {
        this.volume = volume;

        frequencies = new HashSet<>();
        frequencies.add(frequency);
        volumes = new HashMap<>();
        volumes.put(frequency, volume);
    }

    public Bucket(Double volume, Set<Frequency> frequencies, Map<Frequency, Double> volumes) {
        this.volume = volume;
        this.frequencies = frequencies;
        this.volumes = volumes;
    }

    public Bucket(double volume) {
        this.volume = volume;
        this.frequencies = new HashSet<>();
        this.volumes = new HashMap<>();
    }

    public Bucket add(Bucket bucket) {
        Double newVolume = volume + bucket.volume;

        Set<Frequency> newFrequencies = new HashSet<>();
        newFrequencies.addAll(frequencies);
        newFrequencies.addAll(bucket.frequencies);

        Map<Frequency, Double> newVolumes = new HashMap<>();
        newVolumes.putAll(volumes);
        newVolumes.putAll(bucket.volumes);

        return new Bucket(newVolume, newFrequencies, newVolumes);
    }

    public Bucket multiply(double v) {
        return new Bucket(volume * v, frequencies, volumes);
    }
}
