package gui;

import harmonics.Harmonic;
import javafx.util.Pair;
import notes.FrequencySnapshot;
import notes.NoteFrequencySnapshot;

import java.util.*;

public class SpectrumSnapshotBuilder {
    final SpectrumWindow spectrumWindow;

    public final long sampleCount;
    public final Buckets noteBuckets;
    public final Iterator<Pair<Harmonic, Double>> harmonicHierarchyIterator;
    public final Set<Pair<Integer, Double>> newPairs;

    public SpectrumSnapshotBuilder(long sampleCount, SpectrumWindow spectrumWindow) {
        this.spectrumWindow = spectrumWindow;
        this.sampleCount = sampleCount;

        NoteFrequencySnapshot noteFrequencySnapshot = spectrumWindow.noteManager.getSnapshot(sampleCount);
        FrequencySnapshot frequencySnapshot = noteFrequencySnapshot.frequencySnapshot;
        Set<Double> liveFrequencies = frequencySnapshot.liveFrequencies;

        Map<Double, Double> frequencyVolumeTable = noteFrequencySnapshot.getFrequencyVolumeTable();
        Set<Double> clippedFrequencies = spectrumWindow.clip(liveFrequencies);

        noteBuckets = getNewNoteBuckets(clippedFrequencies, frequencyVolumeTable);
        harmonicHierarchyIterator = spectrumWindow.harmonicCalculator.getHarmonicHierarchyIterator(clippedFrequencies, frequencyVolumeTable, 1000);
        newPairs = new HashSet<>();
    }

    public boolean update() {
        Pair<Harmonic, Double> nextHarmonicVolumePair;
        try {
            nextHarmonicVolumePair = harmonicHierarchyIterator.next();
        } catch (NoSuchElementException e) {
            return true;
        }
        try {
            newPairs.add(new Pair<>(spectrumWindow.getX(nextHarmonicVolumePair.getKey().getFrequency()),
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

    public Buckets getNewNoteBuckets(Set<Double> liveFrequencies, Map<Double, Double> frequencyVolumeTable) {
        Set<Pair<Integer, Double>> noteVolumes = new HashSet<>();
        for (Double frequency : liveFrequencies) {
            int x = spectrumWindow.getX(frequency);
            if (x < 0 || x >= GUI.WIDTH) {
                continue;
            }
            noteVolumes.add(new Pair<>(x, frequencyVolumeTable.get(frequency)));
        }
        return new Buckets(noteVolumes);
    }
}
