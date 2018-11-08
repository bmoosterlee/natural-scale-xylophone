package gui.buckets;

import component.*;

import java.util.AbstractMap;
import java.util.Arrays;

public class PrecalculatedBucketHistoryComponent extends Component {
    private final int size;

    private ImmutableLinkedList<Buckets> history;
    private  Buckets timeAverage;

    private final InputPort<Buckets> inputPort;
    private final OutputPort<Buckets> outputPort;

    private OutputPort<ImmutableLinkedList<Buckets>> historyOutputPort;
    private InputPort<ImmutableLinkedList<Buckets>> newHistoryInputPort;
    private OutputPort<Buckets> timeAverageOutputPort;
    private InputPort<Buckets> newtimeAverageInputPort;
    private OutputPort<Buckets> conditionalOutputPort1;
    private OutputPort<Buckets> conditionalOutputPort2;
    private InputPort<Buckets> conditionalInputPort;

    private PrecalculatedBucketHistoryComponent(BoundedBuffer<Buckets> inputBuffer, BoundedBuffer<Buckets> outputBuffer, int size) {
        this.size = size;
        double multiplier = 1. / size;
        this.history = new ImmutableLinkedList<>();
        this.timeAverage = new Buckets();

        BoundedBuffer<Buckets> inputBuffer1 = new BoundedBuffer<>(1, "history input 1");
        BoundedBuffer<Buckets> inputBuffer2 = new BoundedBuffer<>(1, "history input 2");
        new Broadcast<>(inputBuffer, Arrays.asList(inputBuffer1, inputBuffer2));
        inputPort = new InputPort<>(inputBuffer1);
        outputPort = new OutputPort<>(outputBuffer);

        int capacity = 1;

        BoundedBuffer<Buckets> multipliedBucketsBuffer = new BoundedBuffer<>(capacity, "buckets history - multiply")
        new PipeComponent<>(inputBuffer2, multipliedBucketsBuffer, input -> input.multiply(multiplier));

        BoundedBuffer<Buckets> preparedBucketsBuffer1 = new BoundedBuffer<>(capacity, "history - preparedBuckets 1");
        BoundedBuffer<Buckets> preparedBucketsBuffer2 = new BoundedBuffer<>(capacity, "history - preparedBuckets 2");
        new Broadcast<>(multipliedBucketsBuffer, Arrays.asList(preparedBucketsBuffer1, preparedBucketsBuffer2));

        BoundedBuffer<ImmutableLinkedList<Buckets>> historyInputBuffer = new BoundedBuffer<>(capacity, "history - input");
        BoundedBuffer<AbstractMap.SimpleImmutableEntry<ImmutableLinkedList<Buckets>, Buckets>> pair1 = Pairer.PairerWithOutputBuffer(historyInputBuffer, preparedBucketsBuffer1, capacity, "history - pairer");
        BoundedBuffer<ImmutableLinkedList<Buckets>> newHistoryBuffer = new BoundedBuffer<>(capacity, "history - output");
        new PipeComponent<>(pair1, newHistoryBuffer, input -> input.getKey().add(input.getValue()));

        historyOutputPort = new OutputPort<>(historyInputBuffer);
        newHistoryInputPort = new InputPort<>(newHistoryBuffer);

        BoundedBuffer<Buckets> timeAverageBuffer = new BoundedBuffer<>(capacity, "history - time average input");
        BoundedBuffer<AbstractMap.SimpleImmutableEntry<Buckets, Buckets>> pair2 = Pairer.PairerWithOutputBuffer(timeAverageBuffer, preparedBucketsBuffer2, capacity, "history - pairer2");
        BoundedBuffer<Buckets> newTimeAverageBuffer = new BoundedBuffer<>(capacity, "history - new time average");
        new PipeComponent<>(pair2, newTimeAverageBuffer, input -> input.getKey().add(input.getValue()));

        timeAverageOutputPort = new OutputPort<>(timeAverageBuffer);
        newtimeAverageInputPort = new InputPort<>(newTimeAverageBuffer);

        BoundedBuffer<Buckets> conditionalInputBuffer1 = new BoundedBuffer<>(capacity, "history - conditional input 1");
        BoundedBuffer<Buckets> conditionalInputBuffer2 = new BoundedBuffer<>(capacity, "history - conditional input 2");
        BoundedBuffer<AbstractMap.SimpleImmutableEntry<Buckets, Buckets>> pair3 = Pairer.PairerWithOutputBuffer(conditionalInputBuffer1, conditionalInputBuffer2, capacity, "history - pairer2");
        BoundedBuffer<Buckets> conditionalOutputBuffer = new BoundedBuffer<>(capacity, "history - conditional output");
        new PipeComponent<>(pair3, conditionalOutputBuffer, input -> input.getKey().subtract(input.getValue()));

        conditionalOutputPort1 = new OutputPort<>(conditionalInputBuffer1);
        conditionalOutputPort2 = new OutputPort<>(conditionalInputBuffer2);
        conditionalInputPort = new InputPort<>(conditionalOutputBuffer);

        start();
    }

    @Override
    protected void tick() {
        try {
            inputPort.consume();
            Buckets newBuckets = inputPort.consume();
            Buckets result = addNewBuckets(newBuckets);
            outputPort.produce(result);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Buckets addNewBuckets(Buckets newBuckets) {
        try {
            historyOutputPort.produce(history);
            timeAverageOutputPort.produce(timeAverage);

            ImmutableLinkedList<Buckets> newHistory = newHistoryInputPort.consume();
            Buckets newTimeAverage = newtimeAverageInputPort.consume();
            
            if (history.size() >= size) {
                conditionalOutputPort1.produce(newTimeAverage);

                AbstractMap.SimpleImmutableEntry<ImmutableLinkedList<Buckets>, Buckets> poll = newHistory.poll();
                Buckets removed = poll.getValue();
                conditionalOutputPort2.produce(removed);

                newHistory = poll.getKey();
                newTimeAverage = conditionalInputPort.consume();
            }

            history = newHistory;
            timeAverage = newTimeAverage;

            return timeAverage;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }

}