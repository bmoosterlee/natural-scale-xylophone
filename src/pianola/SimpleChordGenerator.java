package pianola;

import frequency.Frequency;
import gui.buckets.*;
import gui.spectrum.SpectrumWindow;
import main.BoundedBuffer;
import main.InputPort;
import main.OutputPort;

import java.util.*;
import java.util.Map.Entry;

public class SimpleChordGenerator {
    private final SpectrumWindow spectrumWindow;
    final int chordSize;
    //todo keep own harmonicsbuckets. To save time, we can copy it from the gui.
    BucketHistory noteHistory = new PrecalculatedBucketHistory(50);
    Frequency[] frequencies;
    final int margin = 80;
    private final int hardLeftBorder;
    private final int hardRightBorder;
    private final int totalMargin;
    private final int repetitionDampener;

    private final OutputPort<Buckets> notesOutput;
    private final InputPort<Buckets> notesInput;

    private final InputPort<Buckets> harmonicsInput;

    public SimpleChordGenerator(BoundedBuffer<Buckets> harmonicsBuffer, int chordSize, Frequency centerFrequency, int totalMargin, int hardLeftBorder, int hardRightBorder, int repetitionDampener, SpectrumWindow spectrumWindow) {
        this.chordSize = chordSize;
        this.totalMargin = totalMargin;
        this.hardLeftBorder = hardLeftBorder;
        this.hardRightBorder = hardRightBorder;
        this.spectrumWindow = spectrumWindow;
        this.repetitionDampener = repetitionDampener;

        BoundedBuffer<Buckets> notesAveragerInputBuffer = new BoundedBuffer<>(1);
        notesOutput = new OutputPort<>(notesAveragerInputBuffer);

        BoundedBuffer<Buckets> notesAveragerOutputBuffer = new BoundedBuffer<>(1);
        new BucketsAverager(20, notesAveragerInputBuffer, notesAveragerOutputBuffer);

        notesInput = new InputPort<>(notesAveragerOutputBuffer);


        BoundedBuffer<Buckets> harmonicsAveragerBuffer = new BoundedBuffer<>(1);
        new BucketsAverager(10, harmonicsBuffer, harmonicsAveragerBuffer);
        harmonicsInput = new InputPort<>(harmonicsAveragerBuffer);

        frequencies = new Frequency[chordSize];

        int centerX = clipLeft(clipRight(this.spectrumWindow.getX(centerFrequency)));

        for (int i1 = 0; i1 < chordSize; i1++) {
            frequencies[i1] = spectrumWindow.getFrequency((int) (centerX + (-chordSize / 2. + i1) * margin));
        }
    }

    void generateChord() {
        try {
            Buckets notesBeforeAveraging = noteHistory.getTimeAveragedBuckets().multiply(repetitionDampener);
            notesOutput.produce(notesBeforeAveraging);
            Buckets noteBuckets = notesInput.consume();

            Buckets harmonicsBuckets = harmonicsInput.consume();

            Buckets maximaBuckets = findBucketMaxima(noteBuckets, harmonicsBuckets);

            int average = findCenterFrequency();
            Buckets centerMaximaBuckets = maximaBuckets.clip(average-totalMargin/2, average+totalMargin/2);

            Borders borders = new Borders().invoke();
            Integer[] leftBorders = borders.getLeftBorders();
            Integer[] rightBorders = borders.getRightBorders();

            updateNotes(centerMaximaBuckets, leftBorders, rightBorders);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    int findCenterFrequency() {
        int average = 0;
        for (int i = 0; i < chordSize; i++) {
            try {
                average += spectrumWindow.getX(frequencies[i]);
            }
            catch(NullPointerException ignored){

            }
        }
        average /= chordSize;
        return average;
    }

    void updateNotes(Buckets maximaBuckets, Integer[] leftBorders, Integer[] rightBorders) {
        for(int i = 0; i< chordSize; i++) {
            try {
                frequencies[i] = updateNote(maximaBuckets, leftBorders[i], rightBorders[i]);
            }
            catch(NullPointerException ignored){

            }
        }
    }

    Frequency updateNote(Buckets maximaBuckets, Integer leftBorder, Integer rightBorder) {
        Buckets freqProximityBuckets = maximaBuckets.clip(leftBorder, rightBorder);

        PriorityQueue<Entry<Integer, Double>> frequencyHierarchy = prioritizeFrequencies(freqProximityBuckets);

        Integer highestValueIndex = pickHighestValueIndex(frequencyHierarchy);

        Bucket highestValueBucket = freqProximityBuckets.getValue(highestValueIndex);
        Frequency frequency = FirstBucketFrequencyStrategy.getFrequency(highestValueBucket);
        if(frequency==null){
            frequency = spectrumWindow.getFrequency(highestValueIndex);
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
        } while (poll.getValue().equals(pollFirst.getValue()));

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

        Integer[] getLeftBorders() {
            return leftBorders;
        }

        Integer[] getRightBorders() {
            return rightBorders;
        }

        Borders invoke() {
            leftBorders = new Integer[chordSize];
            rightBorders = new Integer[chordSize];

            for (int i = 0; i < chordSize - 1; i++) {
                Integer frequencyX = spectrumWindow.getX(frequencies[i]);
                int frequency2X = spectrumWindow.getX(frequencies[i + 1]);
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