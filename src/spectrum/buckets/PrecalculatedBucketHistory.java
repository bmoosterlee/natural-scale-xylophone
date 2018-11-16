package spectrum.buckets;

import component.buffer.BoundedBuffer;
import component.buffer.InputPort;
import component.buffer.OutputPort;
import component.buffer.SimpleBuffer;
import component.buffer.RunningPipeComponent;

import java.util.AbstractMap;
import java.util.LinkedList;

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

        AbstractMap.SimpleImmutableEntry<BoundedBuffer<Buckets>, BoundedBuffer<Buckets>> addNewBucketsMultiplier = RunningPipeComponent.methodToComponentBuffers(input -> input.multiply(multiplier), capacity, "buckets history - multiply");
        multiplierOutputPort = new OutputPort<>(addNewBucketsMultiplier.getKey());

        LinkedList<BoundedBuffer<Buckets>> preparedBucketsBroadcast = new LinkedList<>(addNewBucketsMultiplier.getValue().broadcast(2, "precalculatedBucketHistory preparedBuckets - broadcast"));
        BoundedBuffer<Buckets> preparedBucketsBuffer1 = preparedBucketsBroadcast.poll();
        BoundedBuffer<Buckets> preparedBucketsBuffer2 = preparedBucketsBroadcast.poll();

        SimpleBuffer<ImmutableLinkedList<Buckets>> historyInputBuffer = new SimpleBuffer<>(capacity, "history - input");
        SimpleBuffer<AbstractMap.SimpleImmutableEntry<ImmutableLinkedList<Buckets>, Buckets>> pair1 = historyInputBuffer.pairWith(preparedBucketsBuffer1);
        SimpleBuffer<ImmutableLinkedList<Buckets>> newHistoryBuffer = new SimpleBuffer<>(capacity, "history - output");
        new RunningPipeComponent<>(pair1, newHistoryBuffer, input -> input.getKey().add(input.getValue()));

        historyOutputPort = new OutputPort<>(historyInputBuffer);
        newHistoryInputPort = newHistoryBuffer.createInputPort();

        SimpleBuffer<Buckets> timeAverageBuffer = new SimpleBuffer<>(capacity, "history - time average input");
        SimpleBuffer<AbstractMap.SimpleImmutableEntry<Buckets, Buckets>> pair2 = timeAverageBuffer.pairWith(preparedBucketsBuffer2);
        SimpleBuffer<Buckets> newTimeAverageBuffer = new SimpleBuffer<>(capacity, "history - new time average");
        new RunningPipeComponent<>(pair2, newTimeAverageBuffer, input -> input.getKey().add(input.getValue()));

        timeAverageOutputPort = new OutputPort<>(timeAverageBuffer);
        newtimeAverageInputPort = newTimeAverageBuffer.createInputPort();

        SimpleBuffer<Buckets> conditionalInputBuffer1 = new SimpleBuffer<>(capacity, "history - conditional input 1");
        SimpleBuffer<Buckets> conditionalInputBuffer2 = new SimpleBuffer<>(capacity, "history - conditional input 2");
        SimpleBuffer<AbstractMap.SimpleImmutableEntry<Buckets, Buckets>> pair3 = conditionalInputBuffer1.pairWith(conditionalInputBuffer2);
        SimpleBuffer<Buckets> conditionalOutputBuffer = new SimpleBuffer<>(capacity, "history - conditional output");
        new RunningPipeComponent<>(pair3, conditionalOutputBuffer, input -> input.getKey().subtract(input.getValue()));

        conditionalOutputPort1 = new OutputPort<>(conditionalInputBuffer1);
        conditionalOutputPort2 = new OutputPort<>(conditionalInputBuffer2);
        conditionalInputPort = conditionalOutputBuffer.createInputPort();
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