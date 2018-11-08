package gui.buckets;

import component.*;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.LinkedList;

public class PrecalculatedBucketHistory implements BucketHistory {
    private final int size;
    private final LinkedList<Buckets> history;

    private final double multiplier;
    private final Buckets timeAverage;
    private OutputPort<Buckets> multiplierOutputPort;
    private OutputPort<LinkedList<Buckets>> historyOutputPort;
    private InputPort<LinkedList<Buckets>> newHistoryInputPort;
    private OutputPort<Buckets> timeAverageOutputPort;
    private InputPort<Buckets> newtimeAverageInputPort;


    public PrecalculatedBucketHistory(int size) {
        this(size, new LinkedList<>(), 1. / size, new Buckets());
    }

    private PrecalculatedBucketHistory(int size, LinkedList<Buckets> history, double multiplier, Buckets timeAverage) {
        this.size = size;
        this.history = history;
        this.multiplier = multiplier;
        this.timeAverage = timeAverage;

        int capacity = 1;

        AbstractMap.SimpleImmutableEntry<BoundedBuffer<Buckets>, BoundedBuffer<Buckets>> addNewBucketsMultiplier = PipeComponent.methodToComponentBuffers(input -> input.multiply(multiplier), capacity, "buckets history - multiply");
        multiplierOutputPort = new OutputPort<>(addNewBucketsMultiplier.getKey());

        BoundedBuffer<Buckets> preparedBucketsBuffer1 = new BoundedBuffer<>(capacity, "history - preparedBuckets 1");
        BoundedBuffer<Buckets> preparedBucketsBuffer2 = new BoundedBuffer<>(capacity, "history - preparedBuckets 2");
        new Broadcast<>(addNewBucketsMultiplier.getValue(), Arrays.asList(preparedBucketsBuffer1, preparedBucketsBuffer2));

        BoundedBuffer<LinkedList<Buckets>> historyInputBuffer = new BoundedBuffer<>(capacity, "history - input");
        BoundedBuffer<AbstractMap.SimpleImmutableEntry<LinkedList<Buckets>, Buckets>> pair1 = Pairer.PairerWithOutputBuffer(historyInputBuffer, preparedBucketsBuffer1, capacity, "history - pairer");
        BoundedBuffer<LinkedList<Buckets>> newHistoryBuffer = new BoundedBuffer<>(capacity, "history - output");
        new PipeComponent<>(pair1, newHistoryBuffer, input -> addToHistory(input.getKey(), input.getValue()));

        historyOutputPort = new OutputPort<>(historyInputBuffer);
        newHistoryInputPort = new InputPort<>(newHistoryBuffer);

        BoundedBuffer<Buckets> timeAverageBuffer = new BoundedBuffer<>(capacity, "history - time average input");
        BoundedBuffer<AbstractMap.SimpleImmutableEntry<Buckets, Buckets>> pair2 = Pairer.PairerWithOutputBuffer(timeAverageBuffer, preparedBucketsBuffer2, capacity, "history - pairer2");
        BoundedBuffer<Buckets> newTimeAverageBuffer = new BoundedBuffer<>(capacity, "history - new time average");
        new PipeComponent<>(pair2, newTimeAverageBuffer, input -> addToTimeAverage(input.getKey(), input.getValue()));

        timeAverageOutputPort = new OutputPort<>(timeAverageBuffer);
        newtimeAverageInputPort = new InputPort<>(newTimeAverageBuffer);
    }

    @Override
    public BucketHistory addNewBuckets(Buckets newBuckets) {
        try {
            LinkedList<Buckets> history = new LinkedList<>(this.history);

            multiplierOutputPort.produce(newBuckets);
            historyOutputPort.produce(history);
            timeAverageOutputPort.produce(timeAverage);

            LinkedList<Buckets> newHistory = newHistoryInputPort.consume();
            Buckets newTimeAverage = newtimeAverageInputPort.consume();

            if (this.history.size() >= size) {
                Buckets removed = newHistory.pollFirst();
                newTimeAverage = newTimeAverage.subtract(removed);
            }

            return new PrecalculatedBucketHistory(size, newHistory, multiplier, newTimeAverage);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }

    private Buckets addToTimeAverage(Buckets newTimeAverage, Buckets preparedNewBuckets) {
        newTimeAverage = newTimeAverage.add(preparedNewBuckets);
        return newTimeAverage;
    }

    private static LinkedList<Buckets> addToHistory(LinkedList<Buckets> history, Buckets preparedNewBuckets) {
        LinkedList<Buckets> newHistory = new LinkedList<>(history);
        newHistory.addLast(preparedNewBuckets);
        return newHistory;
    }

    @Override
    public Buckets getTimeAveragedBuckets() {
        return timeAverage;
    }

}