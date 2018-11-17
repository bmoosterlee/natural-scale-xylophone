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

            SimpleBuffer<Buckets> outputTimeAverageBuffer = new SimpleBuffer<>(capacity, "output time average");
            LinkedList<BoundedBuffer<Buckets>> outputTimeAverageBroadcast = new LinkedList<>(outputTimeAverageBuffer.broadcast(2, "precalculatedBucketHistoryComponent output - broadcast"));
            BoundedBuffer<Buckets> timeAverageBuffer = outputTimeAverageBroadcast.poll();

            new OldHistoryRemover(
                historyBuffer
                    .pairWith(
                        preparedBucketsBroadcast.poll())
                    .performMethod(
                        input1 ->
                            input1.getKey()
                            .add(input1.getValue()), "precalculated bucket history - add new buckets"),
                timeAverageBuffer
                    .pairWith(
                        preparedBucketsBroadcast.poll())
                    .performMethod(
                        input1 ->
                            input1.getKey()
                            .add(input1.getValue()), "precalculated bucket history component - add new buckets to time average"),
                historyBuffer,
                outputTimeAverageBuffer,
                size);

            try {
                historyBuffer.createOutputPort().produce(new ImmutableLinkedList<>());
                timeAverageBuffer.createOutputPort().produce(new Buckets());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return outputTimeAverageBroadcast.poll();
        };
    }

    private static class OldHistoryRemover {
        private final int size;

        private final InputPort<ImmutableLinkedList<Buckets>> historyInputPort;
        private final InputPort<Buckets> timeAverageInputPort;

        private final OutputPort<Buckets> conditionalSubtractOutputPort1;
        private final OutputPort<Buckets> conditionalSubtractOutputPort2;
        private final InputPort<Buckets> conditionalSubtractInputPort;

        private final OutputPort<ImmutableLinkedList<Buckets>> historyOutputBufferPort;
        private final OutputPort<Buckets> timeAverageOutputOutputPort;

        OldHistoryRemover(BoundedBuffer<ImmutableLinkedList<Buckets>> historyBuffer, BoundedBuffer<Buckets> timeAverageBuffer, BoundedBuffer<ImmutableLinkedList<Buckets>> historyOutputBuffer, BoundedBuffer<Buckets> timeAverageOutputBuffer, int size) {
            this.size = size;
            historyInputPort = historyBuffer.createInputPort();
            timeAverageInputPort = timeAverageBuffer.createInputPort();

            conditionalSubtractOutputPort1 = new OutputPort<>();
            conditionalSubtractOutputPort2 = new OutputPort<>();

            BoundedBuffer<Buckets> subtractInputBuffer1 = conditionalSubtractOutputPort1.getBuffer();
            BoundedBuffer<Buckets> subtractInputBuffer2 = conditionalSubtractOutputPort2.getBuffer();

            conditionalSubtractInputPort =
                subtractInputBuffer1
                .pairWith(
                    subtractInputBuffer2)
                .performMethod(
                    input1 ->
                        input1.getKey()
                        .subtract(input1.getValue()), "precalculated bucket history component - subtract")
                .createInputPort();

            historyOutputBufferPort = historyOutputBuffer.createOutputPort();
            timeAverageOutputOutputPort = timeAverageOutputBuffer.createOutputPort();

            new TickRunner(){
                @Override
                protected void tick() {
                    OldHistoryRemover.this.tick();
                }
            }.start();
        }

        private void tick(){
            try {
                ImmutableLinkedList<Buckets> history = historyInputPort.consume();
                Buckets timeAverage = timeAverageInputPort.consume();

                if (history.size() >= size) {
                    AbstractMap.SimpleImmutableEntry<ImmutableLinkedList<Buckets>, Buckets> poll = history.poll();
                    history = poll.getKey();

                    conditionalSubtractOutputPort1.produce(timeAverage);
                    Buckets removed = poll.getValue();
                    conditionalSubtractOutputPort2.produce(removed);

                    timeAverage = conditionalSubtractInputPort.consume();
                }

                historyOutputBufferPort.produce(history);
                timeAverageOutputOutputPort.produce(timeAverage);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}