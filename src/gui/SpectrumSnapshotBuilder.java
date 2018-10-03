package gui;

import harmonics.Harmonic;
import frequency.Frequency;
import frequency.FrequencyState;
import time.PerformanceTracker;
import time.TimeKeeper;

import java.util.*;
import java.util.Map.Entry;

public class SpectrumSnapshotBuilder {
    final SpectrumWindow spectrumWindow;

    public final long sampleCount;
    public final Buckets noteBuckets;
    public final Iterator<Entry<Harmonic, Double>> harmonicHierarchyIterator;
    public final Map<Frequency, Double> newPairs;
    public final Set<Frequency> frequencies;

    public SpectrumSnapshotBuilder(long sampleCount, SpectrumWindow spectrumWindow) {
        this.spectrumWindow = spectrumWindow;
        this.sampleCount = sampleCount;

        FrequencyState frequencyState = spectrumWindow.noteManager.getFrequencyState(sampleCount);
        Set<Frequency> liveFrequencies = frequencyState.getFrequencies();

        Map<Frequency, Double> frequencyVolumeTable = frequencyState.getFrequencyVolumeTable(sampleCount);
        Set<Frequency> clippedFrequencies = spectrumWindow.clip(liveFrequencies);

        noteBuckets = toBuckets(clippedFrequencies, frequencyVolumeTable);
        harmonicHierarchyIterator = spectrumWindow.harmonicCalculator.getHarmonicHierarchyIterator(clippedFrequencies, frequencyVolumeTable, 100);
        newPairs = new HashMap<>();
        frequencies = new HashSet<>();
    }

    public boolean update() {
        try {
            Entry<Harmonic, Double> harmonicVolume = harmonicHierarchyIterator.next();
            Frequency frequency = harmonicVolume.getKey().getFrequency();
            frequencies.add(frequency);
            newPairs.put(frequency, harmonicVolume.getValue());
        } catch (NoSuchElementException e) {
            return true;
        }
        return false;
    }

    public SpectrumSnapshot finish() {
        TimeKeeper timeKeeper = PerformanceTracker.startTracking("paintComponent 3 1");
        Buckets newHarmonicsBuckets = toBuckets(frequencies, newPairs).clip(0, GUI.WIDTH);
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("paintComponent 3 2");
        spectrumWindow.getBucketHistory().addNewBuckets(newHarmonicsBuckets);
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("paintComponent 3 3");
        Buckets timeAveragedBuckets = spectrumWindow.getBucketHistory().getTimeAveragedBuckets();
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("paintComponent 3 4");
        SpectrumSnapshot spectrumSnapshot = new SpectrumSnapshot(sampleCount, noteBuckets, timeAveragedBuckets);
        PerformanceTracker.stopTracking(timeKeeper);

        return spectrumSnapshot;
    }

    protected Buckets toBuckets(Set<Frequency> keys, Map<Frequency, Double> map){
        Set<Integer> indices = new HashSet<>();
        Map<Integer, Bucket> entries = new HashMap<>();

        for(Frequency frequency : keys){
            int x = spectrumWindow.getX(frequency);

            Bucket bucket = new Bucket(frequency, map.get(frequency));

            Buckets.fill(indices, entries, x, bucket);
        }
        return new Buckets(indices, entries);
    }

}
