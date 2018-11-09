package spectrum.buckets;

import component.*;

import java.util.AbstractMap;
import java.util.Arrays;

public class PrecalculatedBucketHistoryComponent {

    public PrecalculatedBucketHistoryComponent(BoundedBuffer<Buckets> inputBuffer, BoundedBuffer<Buckets> outputBuffer, int size) {
        int capacity = 100;

        double multiplier = 1. / size;
        BoundedBuffer<Buckets> multipliedBucketsBuffer = new BoundedBuffer<>(capacity, "buckets history - multiply");
        new PipeComponent<>(inputBuffer, multipliedBucketsBuffer, input -> input.multiply(multiplier));

        BoundedBuffer<Buckets> preparedBucketsBuffer1 = new BoundedBuffer<>(capacity, "history - preparedBuckets 1");
        BoundedBuffer<Buckets> preparedBucketsBuffer2 = new BoundedBuffer<>(capacity, "history - preparedBuckets 2");
        new Broadcast<>(multipliedBucketsBuffer, Arrays.asList(preparedBucketsBuffer1, preparedBucketsBuffer2));

        BoundedBuffer<ImmutableLinkedList<Buckets>> historyBuffer = new BoundedBuffer<>(capacity, "history - input");
        OutputPort<ImmutableLinkedList<Buckets>> historyOutputPort = new OutputPort<>(historyBuffer);
        BoundedBuffer<AbstractMap.SimpleImmutableEntry<ImmutableLinkedList<Buckets>, Buckets>> pair1 = Pairer.PairerWithOutputBuffer(historyBuffer, preparedBucketsBuffer1, capacity, "history - pairer");
        BoundedBuffer<ImmutableLinkedList<Buckets>> newHistoryBuffer = new BoundedBuffer<>(capacity, "history - output");
        new PipeComponent<>(pair1, newHistoryBuffer, input -> input.getKey().add(input.getValue()));

        BoundedBuffer<Buckets> timeAverageBuffer = new BoundedBuffer<>(capacity, "history - time average input");
        OutputPort<Buckets> timeAverageOutputPort = new OutputPort<>(timeAverageBuffer);
        BoundedBuffer<AbstractMap.SimpleImmutableEntry<Buckets, Buckets>> pair2 = Pairer.PairerWithOutputBuffer(timeAverageBuffer, preparedBucketsBuffer2, capacity, "history - pairer2");
        BoundedBuffer<Buckets> newTimeAverageBuffer = new BoundedBuffer<>(capacity, "history - new time average");
        new PipeComponent<>(pair2, newTimeAverageBuffer, input -> input.getKey().add(input.getValue()));

        BoundedBuffer<AbstractMap.SimpleImmutableEntry<ImmutableLinkedList<Buckets>, Buckets>> removerPair = Pairer.PairerWithOutputBuffer(newHistoryBuffer, newTimeAverageBuffer, capacity, "remover - input");
        BoundedBuffer<AbstractMap.SimpleImmutableEntry<ImmutableLinkedList<Buckets>, Buckets>> removerOutputBuffer = new BoundedBuffer<>(capacity, "remover - output");
        CallableWithArguments<AbstractMap.SimpleImmutableEntry<ImmutableLinkedList<Buckets>, Buckets>, AbstractMap.SimpleImmutableEntry<ImmutableLinkedList<Buckets>, Buckets>> oldHistoryRemover = new CallableWithArguments<>() {
            private final OutputPort<Buckets> conditionalSubtractOutputPort1;
            private final OutputPort<Buckets> conditionalSubtractOutputPort2;
            private final InputPort<Buckets> conditionalSubtractInputPort;

            {
                BoundedBuffer<Buckets> subtractInputBuffer1 = new BoundedBuffer<>(capacity, "history - conditional input 1");
                BoundedBuffer<Buckets> subtractInputBuffer2 = new BoundedBuffer<>(capacity, "history - conditional input 2");
                BoundedBuffer<AbstractMap.SimpleImmutableEntry<Buckets, Buckets>> subtractInputBuffer = Pairer.PairerWithOutputBuffer(subtractInputBuffer1, subtractInputBuffer2, capacity, "history - pairer2");
                BoundedBuffer<Buckets> subtractOutputBuffer = new BoundedBuffer<>(capacity, "history - conditional output");
                new PipeComponent<>(subtractInputBuffer, subtractOutputBuffer, input -> input.getKey().subtract(input.getValue()));

                conditionalSubtractOutputPort1 = new OutputPort<>(subtractInputBuffer1);
                conditionalSubtractOutputPort2 = new OutputPort<>(subtractInputBuffer2);
                conditionalSubtractInputPort = new InputPort<>(subtractOutputBuffer);
            }

            @Override
            public AbstractMap.SimpleImmutableEntry<ImmutableLinkedList<Buckets>, Buckets> call(AbstractMap.SimpleImmutableEntry<ImmutableLinkedList<Buckets>, Buckets> input) {
                ImmutableLinkedList<Buckets> newHistory = input.getKey();
                Buckets newTimeAverage = input.getValue();
                try {
                    if (newHistory.size() >= size) {
                        conditionalSubtractOutputPort1.produce(newTimeAverage);

                        AbstractMap.SimpleImmutableEntry<ImmutableLinkedList<Buckets>, Buckets> poll = newHistory.poll();
                        Buckets removed = poll.getValue();
                        conditionalSubtractOutputPort2.produce(removed);

                        newHistory = poll.getKey();
                        newTimeAverage = conditionalSubtractInputPort.consume();
                    }
                    return new AbstractMap.SimpleImmutableEntry<>(newHistory, newTimeAverage);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        new PipeComponent<>(removerPair, removerOutputBuffer, oldHistoryRemover);

        BoundedBuffer<Buckets> outputTimeAverageBuffer = new BoundedBuffer<>(capacity, "output time average");
        new Unpairer<>(removerOutputBuffer, historyBuffer, outputTimeAverageBuffer);

        new Broadcast<>(outputTimeAverageBuffer, Arrays.asList(outputBuffer, timeAverageBuffer));

        try {
            historyOutputPort.produce(new ImmutableLinkedList<>());
            timeAverageOutputPort.produce(new Buckets());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

}