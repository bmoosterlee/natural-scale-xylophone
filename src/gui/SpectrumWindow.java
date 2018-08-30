package gui;

import harmonics.Harmonic;
import harmonics.HarmonicCalculator;
import javafx.util.Pair;
import notes.*;

import java.io.Serializable;
import java.util.*;

public class SpectrumWindow {
    private NoteManager noteManager;
    HarmonicCalculator harmonicCalculator;

    Buckets noteBuckets;
    public final BucketHistory bucketHistory = new BucketHistory(100);
    public final double centerFrequency = 2 * 261.63;
    double octaveRange = 3.;
    public double lowerBound;
    public double upperBound;
    double logFrequencyMultiplier;
    double logFrequencyAdditive;
    double xMultiplier;
    private Iterator<Pair<Harmonic, Double>> harmonicHierarchyIterator;
    private Set<Pair<Integer, Double>> newPairs;

    public SpectrumWindow(NoteManager noteManager, HarmonicCalculator harmonicCalculator) {
        this.noteManager = noteManager;
        this.harmonicCalculator = harmonicCalculator;

        lowerBound = centerFrequency / Math.pow(2, octaveRange / 2);
        upperBound = centerFrequency * Math.pow(2, octaveRange / 2);

        double logLowerBound = Math.log(lowerBound);
        double logUpperBound = Math.log(upperBound);
        double logRange = logUpperBound - logLowerBound;
        logFrequencyMultiplier = gui.GUI.WIDTH / logRange;
        logFrequencyAdditive = logLowerBound * gui.GUI.WIDTH / logRange;
        xMultiplier = logRange / gui.GUI.WIDTH;

        noteBuckets = new Buckets();
    }

    boolean update() {
        Pair<Harmonic, Double> nextHarmonicVolumePair;
        try {
            nextHarmonicVolumePair = harmonicHierarchyIterator.next();
        }
        catch(NoSuchElementException e){
            return true;
        }
        try {
            newPairs.add(new Pair<>(getX(nextHarmonicVolumePair.getKey().getFrequency()),
                    nextHarmonicVolumePair.getValue()));
        }
        catch(NullPointerException e){
            return true;
        }
        return false;
    }

    void finishUpdate() {
        Buckets newHarmonicsBuckets = new Buckets(newPairs).clip(0, gui.GUI.WIDTH);
        bucketHistory.addNewBuckets(newHarmonicsBuckets);
    }

    void setupUpdate(long frameStartTime) {
        NoteFrequencySnapshot noteFrequencySnapshot = noteManager.getSnapshot();
        NoteSnapshot noteSnapshot = noteFrequencySnapshot.noteSnapshot;
        HashSet<Note> liveNotes = noteSnapshot.liveNotes;
        HashMap<Note, Envelope> envelopes = noteSnapshot.envelopes;
        FrequencySnapshot frequencySnapshot = noteFrequencySnapshot.frequencySnapshot;
        Set<Double> liveFrequencies = frequencySnapshot.liveFrequencies;
        Map<Double, Set<Note>> frequencyNoteTable = frequencySnapshot.frequencyNoteTable;

        Map<Note, Double> volumeTable = noteManager.getVolumeTable(frameStartTime, liveNotes, envelopes);
        Map<Double, Double> frequencyVolumeTable = noteManager.getFrequencyVolumeTable(frequencyNoteTable, volumeTable);

        noteBuckets = getNewNoteBuckets(liveFrequencies, frequencyVolumeTable);

        harmonicHierarchyIterator = harmonicCalculator.getHarmonicHierarchyIterator(liveFrequencies, frequencyVolumeTable, 1000);
        newPairs = new HashSet<>();
    }

    Buckets getNewNoteBuckets(Set<Double> liveFrequencies, Map<Double, Double> volumeTable) {
        Set<Pair<Integer, Double>> noteVolumes = new HashSet<Pair<Integer, Double>>();
        for (Double frequency : liveFrequencies) {
            int x = getX(frequency);
            if (x < 0 || x >= gui.GUI.WIDTH) {
                continue;
            }
            noteVolumes.add(new Pair(x, volumeTable.get(frequency)));
        }
        return new Buckets(noteVolumes);
    }

    public int getX(double frequency) {
        return (int) (Math.log(frequency) * logFrequencyMultiplier - logFrequencyAdditive);
    }

    public double getFrequency(double x) {
        return Math.exp(x * xMultiplier) * lowerBound;
    }

    public Buckets getNoteBuckets() {
        return noteBuckets;
    }

    public BucketHistory getBucketHistory() {
        return bucketHistory;
    }

    public double getCenterFrequency() {
        return centerFrequency;
    }
}