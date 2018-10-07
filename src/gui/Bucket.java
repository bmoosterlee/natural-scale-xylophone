package gui;

import frequency.Frequency;

import java.util.Map;
import java.util.Set;

public interface Bucket {
    Bucket add(Bucket bucket);

    Bucket multiply(double v);

    Double getVolume();

    Set<Frequency> getFrequencies();

    Map<Frequency, Double> getVolumes();
}
