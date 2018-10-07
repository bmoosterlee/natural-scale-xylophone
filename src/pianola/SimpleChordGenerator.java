package pianola;

import frequency.Frequency;
import gui.*;

import java.util.*;
import java.util.Map.Entry;

public class SimpleChordGenerator {
    private final GUI gui;
    protected final int chordSize;
    //todo keep own harmonicsbuckets. To save time, we can copy it from the gui.
    BucketHistory noteHistory = new PrecalculatedBucketHistory(50);
    protected Frequency[] frequencies;
    protected int margin = 80;
    private int hardLeftBorder;
    private int hardRightBorder;
    private final int totalMargin;
    private int repetitionDampener;

    public SimpleChordGenerator(GUI gui, int chordSize, Frequency centerFrequency, int totalMargin, int hardLeftBorder, int hardRightBorder, int repetitionDampener) {
        this.gui = gui;
        this.chordSize = chordSize;
        this.totalMargin = totalMargin;
        this.hardLeftBorder = hardLeftBorder;
        this.hardRightBorder = hardRightBorder;

        frequencies = new Frequency[chordSize];

        this.repetitionDampener = repetitionDampener;
        int centerX = clipLeft(clipRight(gui.spectrumWindow.getX(centerFrequency)));

        for (int i1 = 0; i1 < chordSize; i1++) {
            frequencies[i1] = gui.getFrequency((int) (centerX + (-chordSize / 2. + i1) * margin));
        }
    }

    void generateChord() {
        SpectrumSnapshot spectrumSnapshot = gui.spectrumSnapshot;

        Buckets noteBuckets = noteHistory.getTimeAveragedBuckets().multiply(repetitionDampener).averageBuckets(20);
        Buckets harmonicsBuckets = spectrumSnapshot.harmonicsBuckets.averageBuckets(10);

        Buckets maximaBuckets = findBucketMaxima(noteBuckets, harmonicsBuckets);

        int average = findCenterFrequency();
        Buckets centerMaximaBuckets = maximaBuckets.clip(average-totalMargin/2, average+totalMargin/2);

        Borders borders = new Borders().invoke();
        Integer[] leftBorders = borders.getLeftBorders();
        Integer[] rightBorders = borders.getRightBorders();

        updateNotes(centerMaximaBuckets, leftBorders, rightBorders);
    }

    protected int findCenterFrequency() {
        int average = 0;
        for (int i = 0; i < chordSize; i++) {
            try {
                average += gui.getX(frequencies[i]);
            }
            catch(NullPointerException ignored){

            }
        }
        average /= chordSize;
        return average;
    }

    protected void updateNotes(Buckets maximaBuckets, Integer[] leftBorders, Integer[] rightBorders) {
        for(int i = 0; i< chordSize; i++) {
            try {
                frequencies[i] = updateNote(maximaBuckets, leftBorders[i], rightBorders[i]);
            }
            catch(NullPointerException e){

            }
        }
    }

    protected Frequency updateNote(Buckets maximaBuckets, Integer leftBorder, Integer rightBorder) {
        Buckets freqProximityBuckets = maximaBuckets.clip(leftBorder, rightBorder);

        PriorityQueue<Entry<Integer, Double>> frequencyHierarchy = prioritizeFrequencies(freqProximityBuckets);

        Integer highestValueIndex = pickHighestValueIndex(frequencyHierarchy);

        Bucket highestValueBucket = freqProximityBuckets.getValue(highestValueIndex);
        Frequency frequency = FirstBucketFrequencyStrategy.getFrequency(highestValueBucket);
        if(frequency==null){
            frequency = gui.getFrequency(highestValueIndex);
        }

        return frequency;
    }

    private PriorityQueue<Entry<Integer, Double>> prioritizeFrequencies(Buckets freqProximityBuckets) {
        PriorityQueue<Entry<Integer, Double>> frequencyHierarchy = new PriorityQueue<>(
                (o1, o2) -> -Double.compare(o1.getValue(), o2.getValue()));
        Iterator<Entry<Integer, Bucket>> iterator = freqProximityBuckets.iterator();
        while (iterator.hasNext()) {
            Entry<Integer, Bucket> pair = iterator.next();
            frequencyHierarchy.add(new AbstractMap.SimpleImmutableEntry<>(pair.getKey(), pair.getValue().getVolume()));
        }
        return frequencyHierarchy;
    }

    private Buckets findBucketMaxima(Buckets noteBuckets, Buckets harmonicsBuckets) {
        Buckets differenceBuckets = harmonicsBuckets.subtract(noteBuckets);
        return differenceBuckets.findMaxima();
    }

    private Integer pickHighestValueIndex(PriorityQueue<Entry<Integer, Double>> frequencyHierarchy) {
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

    public Frequency[] getFrequencies() {
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
                Integer frequencyX = gui.getX(frequencies[i]);
                Integer frequency2X = gui.getX(frequencies[i + 1]);
                int middleBorder = (frequencyX + frequency2X) / 2;
                int marginBorder = frequencyX + margin;

                if (middleBorder < marginBorder) {
                    rightBorders[i]     = middleBorder;
                    leftBorders[i + 1]  = middleBorder;
                } else {
                    rightBorders[i] = marginBorder;
                    leftBorders[i + 1]  = frequency2X - margin;
                }
            }
            int averageFrequency = findCenterFrequency();
            leftBorders[0]              = clipLeft(clipRight( averageFrequency - totalMargin / 2));
            rightBorders[chordSize - 1] = clipLeft(clipRight(averageFrequency + totalMargin / 2));
            return this;
        }

    }
}