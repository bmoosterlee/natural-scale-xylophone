package spectrum.buckets;

import component.*;

import java.util.AbstractMap;
import java.util.Arrays;

public class PrecalculatedBucketHistory implements BucketHistory {
    private final int size;

    private ImmutableLinkedList<Buckets> history;
    private  Buckets timeAverage;

    private OutputPort<Buckets> multiplierOutputPort;
    private OutputPort<ImmutableLinkedList<Buckets>> historyOutputPort;
    private InputPort<ImmutableLinkedList<Buckets>> newHistoryInputPort;
    private OutputPort<Buckets> timeAverageOutputPort;
    private InputPort<Buckets> newtimeAverageInputPort;
    private OutputPort<Buckets> conditionalOutputPort1;
    private OutputPort<Buckets> conditionalOutputPort2;
    private InputPort<Buckets> conditionalInputPort;


    public PrecalculatedBucketHistory(int size) {
        this(size, new ImmutableLinkedList<>(), 1. / size, new Buckets());
    }

    private PrecalculatedBucketHistory(int size, ImmutableLinkedList<Buckets> history, double multiplier, Buckets timeAverage) {
        this.size = size;
        this.history = history;
        this.timeAverage = timeAverage;

        int capacity = 1;

        AbstractMap.SimpleImmutableEntry<BoundedBuffer<Buckets>, BoundedBuffer<Buckets>> addNewBucketsMultiplier = TickablePipeComponent.methodToComponentBuffers(input -> input.multiply(multiplier), capacity, "buckets history - multiply");
        multiplierOutputPort = new OutputPort<>(addNewBucketsMultiplier.getKey());

        BoundedBuffer<Buckets> preparedBucketsBuffer1 = new BoundedBuffer<>(capacity, "history - preparedBuckets 1");
        BoundedBuffer<Buckets> preparedBucketsBuffer2 = new BoundedBuffer<>(capacity, "history - preparedBuckets 2");
        new Broadcast<>(addNewBucketsMultiplier.getValue(), Arrays.asList(preparedBucketsBuffer1, preparedBucketsBuffer2));

        BoundedBuffer<ImmutableLinkedList<Buckets>> historyInputBuffer = new BoundedBuffer<>(capacity, "history - input");
        BoundedBuffer<AbstractMap.SimpleImmutableEntry<ImmutableLinkedList<Buckets>, Buckets>> pair1 = historyInputBuffer.pairWith(preparedBucketsBuffer1);
        BoundedBuffer<ImmutableLinkedList<Buckets>> newHistoryBuffer = new BoundedBuffer<>(capacity, "history - output");
        new TickablePipeComponent<>(pair1, newHistoryBuffer, input -> input.getKey().add(input.getValue()));

        historyOutputPort = new OutputPort<>(historyInputBuffer);
        newHistoryInputPort = new InputPort<>(newHistoryBuffer);

        BoundedBuffer<Buckets> timeAverageBuffer = new BoundedBuffer<>(capacity, "history - time average input");
        BoundedBuffer<AbstractMap.SimpleImmutableEntry<Buckets, Buckets>> pair2 = timeAverageBuffer.pairWith(preparedBucketsBuffer2);
        BoundedBuffer<Buckets> newTimeAverageBuffer = new BoundedBuffer<>(capacity, "history - new time average");
        new TickablePipeComponent<>(pair2, newTimeAverageBuffer, input -> input.getKey().add(input.getValue()));

        timeAverageOutputPort = new OutputPort<>(timeAverageBuffer);
        newtimeAverageInputPort = new InputPort<>(newTimeAverageBuffer);

        BoundedBuffer<Buckets> conditionalInputBuffer1 = new BoundedBuffer<>(capacity, "history - conditional input 1");
        BoundedBuffer<Buckets> conditionalInputBuffer2 = new BoundedBuffer<>(capacity, "history - conditional input 2");
        BoundedBuffer<AbstractMap.SimpleImmutableEntry<Buckets, Buckets>> pair3 = conditionalInputBuffer1.pairWith(conditionalInputBuffer2);
        BoundedBuffer<Buckets> conditionalOutputBuffer = new BoundedBuffer<>(capacity, "history - conditional output");
        new TickablePipeComponent<>(pair3, conditionalOutputBuffer, input -> input.getKey().subtract(input.getValue()));

        conditionalOutputPort1 = new OutputPort<>(conditionalInputBuffer1);
        conditionalOutputPort2 = new OutputPort<>(conditionalInputBuffer2);
        conditionalInputPort = new InputPort<>(conditionalOutputBuffer);
    }

    @Override
    public BucketHistory addNewBuckets(Buckets newBuckets) {
        try {
            multiplierOutputPort.produce(newBuckets);
            historyOutputPort.produce(history);
            timeAverageOutputPort.produce(timeAverage);

            ImmutableLinkedList<Buckets> newHistory = newHistoryInputPort.consume();
            Buckets newTimeAverage = newtimeAverageInputPort.consume();

            if (history.size() >= size) {
                AbstractMap.SimpleImmutableEntry<ImmutableLinkedList<Buckets>, Buckets> poll = newHistory.poll();
                newHistory = poll.getKey();
                Buckets removed = poll.getValue();

                conditionalOutputPort1.produce(newTimeAverage);
                conditionalOutputPort2.produce(removed);
                newTimeAverage = conditionalInputPort.consume();
            }

            history = newHistory;
            timeAverage = newTimeAverage;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return this;
    }

    private Buckets addToTimeAverage(Buckets newTimeAverage, Buckets preparedNewBuckets) {
        newTimeAverage = newTimeAverage.add(preparedNewBuckets);
        return newTimeAverage;
    }

    private static ImmutableLinkedList<Buckets> addToHistory(ImmutableLinkedList<Buckets> history, Buckets preparedNewBuckets) {
        return history.add(preparedNewBuckets);
    }

    @Override
    public Buckets getTimeAveragedBuckets() {
        return timeAverage;
    }

}