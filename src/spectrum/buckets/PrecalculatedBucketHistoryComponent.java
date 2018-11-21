package spectrum.buckets;

import component.buffer.*;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

public class PrecalculatedBucketHistoryComponent extends MethodPipeComponent<Buckets, Buckets> {

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
            SimpleBuffer<Buckets> timeAverageBuffer = new SimpleBuffer<>(capacity, "output time average");

            LinkedList<BoundedBuffer<Buckets>> timeAverageBroadcast = new LinkedList<>(timeAverageBuffer.broadcast(2, "precalculatedBucketHistoryComponent output - broadcast"));

            new OldHistoryRemover(
                historyBuffer
                    .pairWith(
                        preparedBucketsBroadcast.poll())
                    .performMethod(
                        input1 ->
                            input1.getKey()
                            .add(input1.getValue()), "precalculated bucket history - add new buckets"),
                timeAverageBroadcast.poll()
                    .pairWith(
                        preparedBucketsBroadcast.poll())
                    .performMethod(
                        input1 ->
                            input1.getKey()
                            .add(input1.getValue()), "precalculated bucket history component - add new buckets to time average"),
                historyBuffer,
                timeAverageBuffer,
                size);

            try {
                historyBuffer.createOutputPort().produce(new ImmutableLinkedList<>());
                timeAverageBuffer.createOutputPort().produce(new Buckets());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return timeAverageBroadcast.poll();
        };
    }

    private static class OldHistoryRemover {
        private final int size;

        private final InputPort<ImmutableLinkedList<Buckets>> historyInputPort;
        private final InputPort<Buckets> timeAverageInputPort;

        private final OutputPort<Buckets> subtractionInput1;
        private final OutputPort<Buckets> subtractionInput2;
        private final InputPort<Buckets> subtractOutput;

        private final OutputPort<ImmutableLinkedList<Buckets>> historyOutputBufferPort;
        private final OutputPort<Buckets> timeAverageOutputOutputPort;

        OldHistoryRemover(BoundedBuffer<ImmutableLinkedList<Buckets>> historyBuffer, BoundedBuffer<Buckets> timeAverageBuffer, BoundedBuffer<ImmutableLinkedList<Buckets>> historyOutputBuffer, BoundedBuffer<Buckets> timeAverageOutputBuffer, int size) {
            this.size = size;
            historyInputPort = historyBuffer.createInputPort();
            timeAverageInputPort = timeAverageBuffer.createInputPort();

            subtractionInput1 = new OutputPort<>("bucket history - subtraction term 1");
            subtractionInput2 = new OutputPort<>("bucket history - subtraction term 2");

            subtractOutput =
                subtractionInput1.getBuffer()
                .pairWith(
                        subtractionInput2.getBuffer())
                .performMethod(
                    input1 ->
                        input1.getKey()
                        .subtract(input1.getValue()), "precalculated bucket history component - subtract")
                .createInputPort();

            historyOutputBufferPort = historyOutputBuffer.createOutputPort();
            timeAverageOutputOutputPort = timeAverageOutputBuffer.createOutputPort();

            new SimpleTickRunner(new AbstractComponent() {
                @Override
                protected Collection<InputPort> getInputPorts() {
                    return Arrays.asList(historyInputPort, timeAverageInputPort, subtractOutput);
                }

                @Override
                protected Collection<OutputPort> getOutputPorts() {
                    return Arrays.asList(subtractionInput1, subtractionInput2, historyOutputBufferPort, timeAverageOutputOutputPort);
                }

                @Override
                protected void tick() {
                    OldHistoryRemover.this.tick();
                }
            }).start();
        }

        private void tick(){
            try {
                ImmutableLinkedList<Buckets> history = historyInputPort.consume();
                Buckets timeAverage = timeAverageInputPort.consume();

                if (history.size() >= size) {
                    AbstractMap.SimpleImmutableEntry<ImmutableLinkedList<Buckets>, Buckets> poll = history.poll();
                    history = poll.getKey();
                    Buckets removed = poll.getValue();

                    subtractionInput1.produce(timeAverage);
                    subtractionInput2.produce(removed);

                    timeAverage = subtractOutput.consume();
                }

                historyOutputBufferPort.produce(history);
                timeAverageOutputOutputPort.produce(timeAverage);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}