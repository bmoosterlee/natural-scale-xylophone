package spectrum.buckets;

import component.*;
import component.buffer.*;
import component.buffer.RunningPipeComponent;

import java.util.AbstractMap;
import java.util.LinkedList;

public class PrecalculatedBucketHistoryComponent extends RunningPipeComponent<Buckets, Buckets> {

    public PrecalculatedBucketHistoryComponent(SimpleBuffer<Buckets> inputBuffer, SimpleBuffer<Buckets> outputBuffer, int size) {
        super(inputBuffer, outputBuffer, toMethod(buildPipe(size)));
    }

    public static CallableWithArguments<BoundedBuffer<Buckets>, BoundedBuffer<Buckets>> buildPipe(int size){
        return inputBuffer -> {
            int capacity = 100;
            double multiplier = 1. / size;

            LinkedList<BoundedBuffer<Buckets>> preparedBucketsBroadcast =
                    new LinkedList<>(
                            inputBuffer
                                    .performMethod(input1 -> input1.multiply(multiplier), "precalculated bucket history component - multiply")
                                    .broadcast(2, "precalculatedNoteHistoryComponent preparedBuckets - broadcast"));

            SimpleBuffer<ImmutableLinkedList<Buckets>> historyBuffer = new SimpleBuffer<>(capacity, "history - input");

            CallableWithArguments<AbstractMap.SimpleImmutableEntry<ImmutableLinkedList<Buckets>, Buckets>, AbstractMap.SimpleImmutableEntry<ImmutableLinkedList<Buckets>, Buckets>> removeOldHistory = new CallableWithArguments<>() {
                private final OutputPort<Buckets> conditionalSubtractOutputPort1;
                private final OutputPort<Buckets> conditionalSubtractOutputPort2;
                private final InputPort<Buckets> conditionalSubtractInputPort;

                {
                    conditionalSubtractOutputPort1 = new OutputPort<>();
                    conditionalSubtractOutputPort2 = new OutputPort<>();

                    BoundedBuffer<Buckets> subtractInputBuffer1 = conditionalSubtractOutputPort1.getBuffer();
                    BoundedBuffer<Buckets> subtractInputBuffer2 = conditionalSubtractOutputPort2.getBuffer();

                    conditionalSubtractInputPort =
                            subtractInputBuffer1
                                    .pairWith(subtractInputBuffer2)
                                    .performMethod(
                                            input1 ->
                                                    input1.getKey()
                                                            .subtract(input1.getValue()), "precalculated bucket history component - subtract")
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

            SimpleBuffer<Buckets> outputTimeAverageBuffer = new SimpleBuffer<>(capacity, "output time average");
            LinkedList<BoundedBuffer<Buckets>> outputTimeAverageBroadcast = new LinkedList<>(outputTimeAverageBuffer.broadcast(2, "precalculatedBucketHistoryComponent output - broadcast"));
            BoundedBuffer<Buckets> timeAverageBuffer = outputTimeAverageBroadcast.poll();

            new Unpairer<>(
                    historyBuffer
                            .pairWith(preparedBucketsBroadcast.poll())
                            .performMethod(
                                    input1 ->
                                            input1.getKey()
                                                    .add(input1.getValue()), "precalculated bucket history - add new buckets")
                            .pairWith(
                                    timeAverageBuffer
                                            .pairWith(
                                                preparedBucketsBroadcast.poll())
                                            .performMethod(
                                                    input1 ->
                                                            input1.getKey()
                                                                    .add(input1.getValue()), "precalculated bucket history component - add new buckets to time average"))
                            .performMethod(removeOldHistory, "precalculated bucket history component - remove old history"),
                    historyBuffer,
                    outputTimeAverageBuffer);

            try {
                historyBuffer.createOutputPort().produce(new ImmutableLinkedList<>());
                timeAverageBuffer.createOutputPort().produce(new Buckets());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return outputTimeAverageBroadcast.poll();
        };
    }

}