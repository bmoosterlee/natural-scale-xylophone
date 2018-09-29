package gui;

import harmonics.Harmonic;
import frequency.Frequency;
import frequency.FrequencyState;

import java.util.*;
import java.util.Map.Entry;

public class SpectrumSnapshotBuilder {
    final SpectrumWindow spectrumWindow;

    public final long sampleCount;
    public final Buckets noteBuckets;
    public final Iterator<Entry<Harmonic, Double>> harmonicHierarchyIterator;
    public final Set<Entry<Integer, Double>> newPairs;

    public SpectrumSnapshotBuilder(long sampleCount, SpectrumWindow spectrumWindow) {
        this.spectrumWindow = spectrumWindow;
        this.sampleCount = sampleCount;

        FrequencyState frequencyState = spectrumWindow.noteManager.getFrequencyState(sampleCount);
        Set<Frequency> liveFrequencies = frequencyState.getFrequencies();

        Map<Frequency, Double> frequencyVolumeTable = frequencyState.getFrequencyVolumeTable(sampleCount);
        Set<Frequency> clippedFrequencies = spectrumWindow.clip(liveFrequencies);

        noteBuckets = getNewNoteBuckets(clippedFrequencies, frequencyVolumeTable);
        harmonicHierarchyIterator = spectrumWindow.harmonicCalculator.getHarmonicHierarchyIterator(clippedFrequencies, frequencyVolumeTable, 100);
        newPairs = new HashSet<>();
    }

    public boolean update() {
        Entry<Harmonic, Double> nextHarmonicVolumePair;
        try {
            nextHarmonicVolumePair = harmonicHierarchyIterator.next();
        } catch (NoSuchElementException e) {
            return true;
        }
        try {
            newPairs.add(new AbstractMap.SimpleImmutableEntry<Integer, Double>(spectrumWindow.getX(nextHarmonicVolumePair.getKey().getFrequency()),
                    nextHarmonicVolumePair.getValue()));
        } catch (NullPointerException e) {
            return true;
        }
        return false;
    }

    public SpectrumSnapshot finish() {
        Buckets newHarmonicsBuckets = new Buckets(newPairs).clip(0, GUI.WIDTH);
        spectrumWindow.getBucketHistory().addNewBuckets(newHarmonicsBuckets);
        return new SpectrumSnapshot(sampleCount, noteBuckets, spectrumWindow.getBucketHistory().getTimeAveragedBuckets());
    }

    public Buckets getNewNoteBuckets(Set<Frequency> liveFrequencies, Map<Frequency, Double> frequencyVolumeTable) {
        Set<Entry<Integer, Double>> noteVolumes = new HashSet<>();
        for (Frequency frequency : liveFrequencies) {
            int x = spectrumWindow.getX(frequency);
            if (x < 0 || x >= GUI.WIDTH) {
                continue;
            }
            noteVolumes.add(new AbstractMap.SimpleImmutableEntry<>(x, frequencyVolumeTable.get(frequency)));
        }
        return new Buckets(noteVolumes);
    }
}
