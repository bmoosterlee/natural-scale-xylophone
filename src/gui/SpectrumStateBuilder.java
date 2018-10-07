package gui;

import gui.buckets.AtomicBucket;
import gui.buckets.Bucket;
import gui.buckets.Buckets;
import harmonics.Harmonic;
import frequency.Frequency;
import frequency.FrequencyState;
import time.PerformanceTracker;
import time.TimeKeeper;

import java.util.*;
import java.util.Map.Entry;

public class SpectrumStateBuilder {
    private final SpectrumWindow spectrumWindow;

    public final long sampleCount;
    private final Buckets noteBuckets;
    private final Iterator<Entry<Harmonic, Double>> harmonicHierarchyIterator;
    private final Map<Frequency, Double> newPairs;
    public final Set<Frequency> frequencies;

    SpectrumStateBuilder(long sampleCount, SpectrumWindow spectrumWindow) {
        this.spectrumWindow = spectrumWindow;
        this.sampleCount = sampleCount;

        FrequencyState frequencyState = spectrumWindow.frequencyManager.getFrequencyState(sampleCount);
        Set<Frequency> liveFrequencies = frequencyState.getFrequencies();

        Map<Frequency, Double> frequencyVolumeTable = frequencyState.getFrequencyVolumeTable(sampleCount);

        noteBuckets = toBuckets(liveFrequencies, frequencyVolumeTable);
        harmonicHierarchyIterator = spectrumWindow.harmonicCalculator.getHarmonicHierarchyIterator(liveFrequencies, frequencyVolumeTable, 100);
        newPairs = new HashMap<>();
        frequencies = new HashSet<>();
    }

    //return true when harmonicHierarchy has been depleted.
    public boolean update() {
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
        TimeKeeper timeKeeper = PerformanceTracker.startTracking("paintComponent 3 1");
        Buckets newHarmonicsBuckets = toBuckets(frequencies, newPairs);
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("paintComponent 3 2");
        spectrumWindow.getBucketHistory().addNewBuckets(newHarmonicsBuckets);
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("paintComponent 3 3");
        Buckets timeAveragedBuckets = spectrumWindow.getBucketHistory().getTimeAveragedBuckets();
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("paintComponent 3 4");
        SpectrumState spectrumState = new SpectrumState(noteBuckets, timeAveragedBuckets);
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
        return new Buckets(indices, entries);
    }

}
