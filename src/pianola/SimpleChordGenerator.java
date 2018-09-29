package pianola;

import frequency.Frequency;
import gui.BucketHistory;
import gui.Buckets;
import gui.GUI;
import gui.SpectrumSnapshot;

import java.util.*;
import java.util.Map.Entry;

public class SimpleChordGenerator {
    private final GUI gui;
    protected final int chordSize;
    //todo keep own harmonicsbuckets. To save time, we can copy it from the gui.
    BucketHistory noteHistory = new BucketHistory(50);
    protected Integer[] frequencies;
    protected int margin = 80;
    private int hardLeftBorder;
    private int hardRightBorder;
    private final int totalMargin;

    public SimpleChordGenerator(GUI gui, int chordSize, Frequency centerFrequency, int totalMargin, int hardLeftBorder, int hardRightBorder) {
        this.gui = gui;
        this.chordSize = chordSize;
        this.totalMargin = totalMargin;
        this.hardLeftBorder = hardLeftBorder;
        this.hardRightBorder = hardRightBorder;

        frequencies = new Integer[chordSize];

        int centerX = clipLeft(clipRight(gui.spectrumWindow.getX(centerFrequency)));

        for (int i1 = 0; i1 < chordSize; i1++) {
            frequencies[i1] = (int) (centerX + (-chordSize / 2. + i1) * margin);
        }
    }

    void generateChord() {
        SpectrumSnapshot spectrumSnapshot = gui.spectrumSnapshot;

        Buckets noteBuckets = noteHistory.getTimeAveragedBuckets().multiply(3).averageBuckets(20);
        Buckets harmonicsBuckets = spectrumSnapshot.harmonicsBuckets.averageBuckets(10);

        Buckets maximaBuckets = findBucketMaxima(noteBuckets, harmonicsBuckets);

        Buckets centerProximity = calculateCenterProximity();

        Borders borders = new Borders().invoke();
        Integer[] leftBorders = borders.getLeftBorders();
        Integer[] rightBorders = borders.getRightBorders();

        updateNotes(maximaBuckets, centerProximity, leftBorders, rightBorders);

        Arrays.sort(frequencies);
    }

    private Buckets calculateCenterProximity() {
        int average = findCenterFrequency();

        return calculateCenterProximity(average);
    }

    private Buckets calculateCenterProximity(int average) {
        Set<Entry<Integer, Double>> nearCenterFrequencies = new HashSet<>();
        for (int x = -totalMargin/2; x < totalMargin/2; x++) {
            nearCenterFrequencies.add(new AbstractMap.SimpleImmutableEntry(average + x, 1.));
        }
        return new Buckets(nearCenterFrequencies);
    }

    protected int findCenterFrequency() {
        int average = 0;
        for (int i = 0; i < chordSize; i++) {
            average += frequencies[i];
        }
        average /= chordSize;
        return average;
    }

    protected void updateNotes(Buckets maximaBuckets, Buckets centerProximity, Integer[] leftBorders, Integer[] rightBorders) {
        for(int i = 0; i< chordSize; i++) {
            updateNote(maximaBuckets, centerProximity, i, leftBorders[i], rightBorders[i]);
        }
    }

    protected void updateNote(Buckets maximaBuckets, Buckets centerProximity, int noteIndex, Integer leftBorder, Integer rightBorder) {
        Buckets nearbyFrequencyBuckets = getNearbyBuckets(leftBorder, rightBorder);

        Buckets freqProximityBuckets = maximaBuckets.multiply(nearbyFrequencyBuckets).multiply(centerProximity);

        PriorityQueue<Entry<Integer, Double>> frequencyHierarchy = prioritizeFrequencies(freqProximityBuckets);

        try {
            Integer key = pickHighestValueHarmonic(frequencyHierarchy);
            frequencies[noteIndex] = key;
        } catch (NullPointerException e) {

        }
    }

    private PriorityQueue<Entry<Integer, Double>> prioritizeFrequencies(Buckets freqProximityBuckets) {
        PriorityQueue<Entry<Integer, Double>> frequencyHierarchy = new PriorityQueue<>(
                (o1, o2) -> -Double.compare(o1.getValue(), o2.getValue()));
        Iterator<Entry<Integer, Double>> iterator = freqProximityBuckets.iterator();
        while (iterator.hasNext()) {
            Entry<Integer, Double> pair = iterator.next();
            frequencyHierarchy.add(pair);
        }
        return frequencyHierarchy;
    }

    private Buckets getNearbyBuckets(Integer leftBorder, Integer rightBorder) {
        Set<Entry<Integer, Double>> nearbyFrequencies = new HashSet<>();
        for (int x = leftBorder; x < rightBorder; x++) {
            nearbyFrequencies.add(new AbstractMap.SimpleImmutableEntry(x, 1.));
        }
        return new Buckets(nearbyFrequencies);
    }

    private Buckets findBucketMaxima(Buckets noteBuckets, Buckets harmonicsBuckets) {
        Buckets differenceBuckets = harmonicsBuckets.subtract(noteBuckets);
        Set<Entry<Integer, Double>> maxima = differenceBuckets.findMaxima();
        return new Buckets(maxima);
    }

    private Integer pickHighestValueHarmonic(PriorityQueue<Entry<Integer, Double>> frequencyHierarchy) {
        ArrayList<Entry<Integer, Double>> topBuckets = new ArrayList<>();
        Entry<Integer, Double> pollFirst = frequencyHierarchy.poll();
        Entry<Integer, Double> poll = pollFirst;
        do {
            topBuckets.add(poll);
            try {
                poll = frequencyHierarchy.poll();
            } catch (NullPointerException e) {
                break;
            }
        } while (poll.getValue() == pollFirst.getValue());

        return topBuckets.get((int) (Math.random() * topBuckets.size())).getKey();
    }

    private int clipLeft(int x) {
        return Math.max(hardLeftBorder,
                x
        );
    }

    private int clipRight(int x) {
        return Math.min(hardRightBorder,
                x
        );
    }

    public Integer[] getFrequencies() {
        return frequencies;
    }

    private class Borders {
        private Integer[] leftBorders;
        private Integer[] rightBorders;

        public Integer[] getLeftBorders() {
            return leftBorders;
        }

        public Integer[] getRightBorders() {
            return rightBorders;
        }

        public Borders invoke() {
            leftBorders = new Integer[chordSize];
            rightBorders = new Integer[chordSize];

            for (int i = 0; i < chordSize - 1; i++) {
                int middleBorder = (frequencies[i] + frequencies[i + 1]) / 2;
                int marginBorder = frequencies[i] + margin;

                if (middleBorder < marginBorder) {
                    rightBorders[i]     = middleBorder;
                    leftBorders[i + 1]  = middleBorder;
                } else {
                    rightBorders[i] = marginBorder;
                    leftBorders[i + 1]  = frequencies[i + 1] - margin;
                }
            }
            int averageFrequency = findCenterFrequency();
            leftBorders[0]              = clipLeft(clipRight( averageFrequency - totalMargin / 2));
            rightBorders[chordSize - 1] = clipLeft(clipRight(averageFrequency + totalMargin / 2));
            return this;
        }

    }
}