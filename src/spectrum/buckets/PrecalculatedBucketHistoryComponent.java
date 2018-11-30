package spectrum.buckets;

import component.Unpairer;
import component.buffer.*;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.LinkedList;

public class PrecalculatedBucketHistoryComponent extends MethodPipeComponent<Buckets, Buckets> {

    public PrecalculatedBucketHistoryComponent(SimpleBuffer<Buckets> inputBuffer, SimpleBuffer<Buckets> outputBuffer, int size) {
        super(inputBuffer, outputBuffer, toMethod(buildPipe(size)));
    }

    public static PipeCallable<BoundedBuffer<Buckets>, BoundedBuffer<Buckets>> buildPipe(int size){
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

            SimpleImmutableEntry<SimpleBuffer<ImmutableLinkedList<Buckets>>, SimpleBuffer<Buckets>> unpair =
                    Unpairer.unpair(
                        historyBuffer
                        .pairWith(
                                preparedBucketsBroadcast.poll())
                        .performMethod(
                                input1 ->
                                        input1.getKey()
                                        .add(input1.getValue()), "precalculated bucket history - add new buckets")
                        .pairWith(
                                timeAverageBroadcast.poll()
                                .pairWith(
                                        preparedBucketsBroadcast.poll())
                                .performMethod(
                                        input1 ->
                                                input1.getKey()
                                                .add(input1.getValue()), "precalculated bucket history component - add new buckets to time average"))
                        .performMethod(removeOldHistory(size)));

            unpair.getKey().relayTo(historyBuffer);
            unpair.getValue().relayTo(timeAverageBuffer);

            try {
                historyBuffer.createOutputPort().produce(new ImmutableLinkedList<>());
                timeAverageBuffer.createOutputPort().produce(new Buckets());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return timeAverageBroadcast.poll();
        };
    }

    public static PipeCallable<SimpleImmutableEntry<ImmutableLinkedList<Buckets>, Buckets>, SimpleImmutableEntry<ImmutableLinkedList<Buckets>, Buckets>> removeOldHistory(int size){
        return input -> {
            ImmutableLinkedList<Buckets> history = input.getKey();
            Buckets timeAverage = input.getValue();

            if (history.size() >= size) {
                SimpleImmutableEntry<ImmutableLinkedList<Buckets>, Buckets> poll = history.poll();
                history = poll.getKey();
                Buckets removed = poll.getValue();
                timeAverage = timeAverage.subtract(removed);
            }

            return new SimpleImmutableEntry<>(history, timeAverage);
        };
    }

}