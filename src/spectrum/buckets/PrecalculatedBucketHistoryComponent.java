package spectrum.buckets;

import component.*;
import component.buffer.*;
import component.buffer.RunningPipeComponent;

import java.util.AbstractMap;
import java.util.LinkedList;

public class PrecalculatedBucketHistoryComponent extends RunningPipeComponent<Buckets, Buckets> {

    public PrecalculatedBucketHistoryComponent(SimpleBuffer<Buckets> inputBuffer, SimpleBuffer<Buckets> outputBuffer, int size) {
        super(inputBuffer, outputBuffer, recordHistory(size));
    }

    public static CallableWithArguments<Buckets, Buckets> recordHistory(int size){
        return new CallableWithArguments<>() {

            private OutputPort<Buckets> methodInput;
            private InputPort<Buckets> methodOutput;

            {
                int capacity = 100;
                double multiplier = 1. / size;

                SimpleBuffer<Buckets> inputBuffer = new SimpleBuffer<>(capacity, "buckets history - input");
                methodInput = inputBuffer.createOutputPort();

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
                        BoundedBuffer<Buckets> subtractInputBuffer1 = new SimpleBuffer<>(capacity, "history - conditional input 1");
                        BoundedBuffer<Buckets> subtractInputBuffer2 = new SimpleBuffer<>(capacity, "history - conditional input 2");

                        conditionalSubtractOutputPort1 = subtractInputBuffer1.createOutputPort();
                        conditionalSubtractOutputPort2 = subtractInputBuffer2.createOutputPort();
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
                    .pairWith(preparedBucketsBroadcast.poll())
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

                methodOutput = outputTimeAverageBroadcast.poll().createInputPort();
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