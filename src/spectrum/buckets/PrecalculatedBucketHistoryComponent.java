package spectrum.buckets;

import component.Unpairer;
import component.buffer.*;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.LinkedList;

public class PrecalculatedBucketHistoryComponent {

    public static <A extends Packet<Buckets>> PipeCallable<BoundedBuffer<Buckets, A>, BoundedBuffer<Buckets, SimplePacket<Buckets>>> buildPipe(int size){
        return inputBuffer -> {
            int capacity = 100;
            double multiplier = 1. / size;

            BufferChainLink<Buckets, SimplePacket<Buckets>> bucketsPacketBufferChainLink = inputBuffer
                    .rewrap()
                    .performMethod(input1 -> input1.multiply(multiplier), "precalculated bucket history component - multiply");
            LinkedList<BoundedBuffer<Buckets, SimplePacket<Buckets>>> preparedBucketsBroadcast =
                new LinkedList<>(
                    bucketsPacketBufferChainLink
                    .broadcast(2, "precalculatedNoteHistoryComponent preparedBuckets - broadcast"));

            SimpleBuffer<ImmutableLinkedList<Buckets>, SimplePacket<ImmutableLinkedList<Buckets>>> historyBuffer = new SimpleBuffer<>(capacity, "history - input");
            SimpleBuffer<Buckets, SimplePacket<Buckets>> timeAverageBuffer = new SimpleBuffer<>(capacity, "output time average");

            LinkedList<BoundedBuffer<Buckets, SimplePacket<Buckets>>> timeAverageBroadcast = new LinkedList<>(timeAverageBuffer.broadcast(2, "precalculatedBucketHistoryComponent output - broadcast"));

            SimpleImmutableEntry<SimpleBuffer<ImmutableLinkedList<Buckets>, SimplePacket<ImmutableLinkedList<Buckets>>>, SimpleBuffer<Buckets, SimplePacket<Buckets>>> unpair =
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
                historyBuffer.createOutputPort().produce(new SimplePacket<>(new ImmutableLinkedList<>()));
                timeAverageBuffer.createOutputPort().produce(new SimplePacket<>(new Buckets()));
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