package spectrum.buckets;

import component.*;

import java.util.AbstractMap;
import java.util.Arrays;

public class PrecalculatedBucketHistoryComponent extends TickablePipeComponent<Buckets, Buckets> {

    public PrecalculatedBucketHistoryComponent(BoundedBuffer<Buckets> inputBuffer, BoundedBuffer<Buckets> outputBuffer, int size) {
        super(inputBuffer, outputBuffer, recordHistory(size));
    }

    public static CallableWithArguments<Buckets, Buckets> recordHistory(int size){
        return new CallableWithArguments<>() {

            private OutputPort<Buckets> methodInput;
            private InputPort<Buckets> methodOutput;

            {
                int capacity = 100;
                double multiplier = 1. / size;

                BoundedBuffer<Buckets> inputBuffer = new BoundedBuffer<>(capacity, "buckets history - input");
                methodInput = inputBuffer.createOutputPort();

                BoundedBuffer<Buckets>[] preparedBucketsBroadcast =
                    inputBuffer
                    .performMethod(input1 -> input1.multiply(multiplier))
                    .broadcast(2).toArray(new BoundedBuffer[0]);
                BoundedBuffer<Buckets> preparedBucketsBuffer1 = preparedBucketsBroadcast[0];
                BoundedBuffer<Buckets> preparedBucketsBuffer2 = preparedBucketsBroadcast[1];

                BoundedBuffer<ImmutableLinkedList<Buckets>> historyBuffer = new BoundedBuffer<>(capacity, "history - input");
                OutputPort<ImmutableLinkedList<Buckets>> historyOutputPort = historyBuffer.createOutputPort();

                CallableWithArguments<AbstractMap.SimpleImmutableEntry<ImmutableLinkedList<Buckets>, Buckets>, AbstractMap.SimpleImmutableEntry<ImmutableLinkedList<Buckets>, Buckets>> removeOldHistory = new CallableWithArguments<>() {
                    private final OutputPort<Buckets> conditionalSubtractOutputPort1;
                    private final OutputPort<Buckets> conditionalSubtractOutputPort2;
                    private final InputPort<Buckets> conditionalSubtractInputPort;

                    {
                        BoundedBuffer<Buckets> subtractInputBuffer1 = new BoundedBuffer<>(capacity, "history - conditional input 1");
                        BoundedBuffer<Buckets> subtractInputBuffer2 = new BoundedBuffer<>(capacity, "history - conditional input 2");

                        conditionalSubtractOutputPort1 = subtractInputBuffer1.createOutputPort();
                        conditionalSubtractOutputPort2 = subtractInputBuffer2.createOutputPort();
                        conditionalSubtractInputPort =
                            subtractInputBuffer1
                            .pairWith(subtractInputBuffer2)
                            .performMethod(
                                input1 ->
                                    input1.getKey()
                                    .subtract(input1.getValue()))
                            .createInputPort();
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

                BoundedBuffer<Buckets> outputTimeAverageBuffer = new BoundedBuffer<>(capacity, "output time average");
                BoundedBuffer<Buckets>[] outputTimeAverageBroadcast = outputTimeAverageBuffer.broadcast(2).toArray(new BoundedBuffer[0]);
                BoundedBuffer<Buckets> outputBuffer = outputTimeAverageBroadcast[0];
                BoundedBuffer<Buckets> timeAverageBuffer = outputTimeAverageBroadcast[1];

                OutputPort<Buckets> timeAverageOutputPort = timeAverageBuffer.createOutputPort();

                new Unpairer<>(
                    historyBuffer
                    .pairWith(preparedBucketsBuffer1)
                    .performMethod(
                        input1 ->
                        input1.getKey()
                        .add(input1.getValue()))
                    .pairWith(
                        timeAverageBuffer
                        .pairWith(preparedBucketsBuffer2)
                        .performMethod(
                            input1 ->
                            input1.getKey()
                            .add(input1.getValue())))
                    .performMethod(removeOldHistory),
                    historyBuffer,
                    outputTimeAverageBuffer);

                try {
                    historyOutputPort.produce(new ImmutableLinkedList<>());
                    timeAverageOutputPort.produce(new Buckets());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                methodOutput = outputBuffer.createInputPort();
            }

            private Buckets recordHistory(Buckets input) {
                try {
                    methodInput.produce(input);
                    return methodOutput.consume();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }


            @Override
            public Buckets call(Buckets input) {
                return recordHistory(input);
            }
        };
    }

}