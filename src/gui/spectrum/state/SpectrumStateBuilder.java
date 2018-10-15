package gui.spectrum.state;

import frequency.Frequency;
import gui.buckets.AtomicBucket;
import gui.buckets.Bucket;
import gui.buckets.Buckets;
import gui.spectrum.SpectrumWindow;
import harmonics.Harmonic;
import time.PerformanceTracker;
import time.TimeKeeper;

import java.util.*;
import java.util.Map.Entry;

class SpectrumStateBuilder {
    private final SpectrumWindow spectrumWindow;

    private final Buckets noteBuckets;
    private final Iterator<Entry<Harmonic, Double>> harmonicHierarchyIterator;
    private final Map<Frequency, Double> newPairs;
    private final Set<Frequency> frequencies;

    SpectrumStateBuilder(SpectrumWindow spectrumWindow, Set<Frequency> liveFrequencies, Map<Frequency, Double> volumes, Iterator<Entry<Harmonic, Double>> harmonicHierarchyIterator) {
        this.spectrumWindow = spectrumWindow;
        this.harmonicHierarchyIterator = harmonicHierarchyIterator;

        noteBuckets = toBuckets(liveFrequencies, volumes).precalculate();
        newPairs = new HashMap<>();
        frequencies = new HashSet<>();
    }

    //return true when harmonicHierarchy has been depleted.
    boolean update() {
        try {
            Entry<Harmonic, Double> harmonicVolume = harmonicHierarchyIterator.next();
            Frequency frequency = harmonicVolume.getKey().getFrequency();

            if(spectrumWindow.inBounds(frequency)) {
                frequencies.add(frequency);
                try {
                    newPairs.put(frequency, newPairs.get(frequency) + harmonicVolume.getValue());
                } catch (NullPointerException e) {
                    newPairs.put(frequency, harmonicVolume.getValue());
                }
            }
        } catch (NoSuchElementException e) {
            return true;
        }
        return false;
    }

    SpectrumState finish() {
        TimeKeeper timeKeeper = PerformanceTracker.startTracking("harmonics toBuckets");
        Buckets newHarmonicsBuckets = toBuckets(frequencies, newPairs);
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("create SpectrumState");
        SpectrumState spectrumState = new SpectrumState(noteBuckets, newHarmonicsBuckets);
        PerformanceTracker.stopTracking(timeKeeper);

        return spectrumState;
    }

    private Buckets toBuckets(Set<Frequency> keys, Map<Frequency, Double> map){
        Set<Integer> indices = new HashSet<>();
        Map<Integer, Bucket> entries = new HashMap<>();

        for(Frequency frequency : keys){
            int x = spectrumWindow.getX(frequency);

            AtomicBucket bucket = new AtomicBucket(frequency, map.get(frequency));

            Buckets.fill(indices, entries, x, bucket);
        }
        return new Buckets(indices, entries).precalculate();
    }

}
