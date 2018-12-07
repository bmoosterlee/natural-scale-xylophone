package main;

import component.buffer.*;
import component.orderer.OrderStamp;
import component.orderer.OrderStampedPacket;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OrderStampedPacketPairer{

    private static <K, V> void tryPairAndProduce(OrderStamp stamp, Map<OrderStamp, OrderStampedPacket<K>> firstMap, Map<OrderStamp, OrderStampedPacket<V>> secondMap, OutputPort<SimpleImmutableEntry<K, V>, OrderStampedPacket<SimpleImmutableEntry<K, V>>> outputPort) throws InterruptedException {
        if (firstMap.containsKey(stamp) && secondMap.containsKey(stamp)) {
            OrderStampedPacket<K> firstInput = firstMap.remove(stamp);
            if(firstInput!=null) {
                OrderStampedPacket<SimpleImmutableEntry<K, V>> pair = firstInput.transform(input1 ->
                        new SimpleImmutableEntry<>(
                                input1,
                                secondMap.remove(stamp).unwrap()));
                outputPort.produce(pair);
            }
        }
    }

    public static <K, V> SimpleBuffer<SimpleImmutableEntry<K, V>, OrderStampedPacket<SimpleImmutableEntry<K, V>>> buildComponent(BoundedBuffer<K, OrderStampedPacket<K>> inputBuffer1, BoundedBuffer<V, OrderStampedPacket<V>> inputBuffer2, String name){
        SimpleBuffer<SimpleImmutableEntry<K, V>, OrderStampedPacket<SimpleImmutableEntry<K, V>>> outputBuffer = new SimpleBuffer<>(1, name);

        final Map<OrderStamp, OrderStampedPacket<K>> input1Map = new ConcurrentHashMap<>();
        final Map<OrderStamp, OrderStampedPacket<V>> input2Map = new ConcurrentHashMap<>();

        new TickRunningStrategy(new AbstractPipeComponent<>(inputBuffer1.createInputPort(), outputBuffer.createOutputPort()) {
            @Override
            protected void tick() {
                try {
                    OrderStampedPacket<K> input1 = input.consume();
                    OrderStamp stamp = input1.stamp;
                    input1Map.put(stamp, input1);
                    tryPairAndProduce(stamp, input1Map, input2Map, output);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        new TickRunningStrategy(new AbstractPipeComponent<>(inputBuffer2.createInputPort(), outputBuffer.createOutputPort()) {
            @Override
            protected void tick() {
                try {
                    OrderStampedPacket<V> input1 = input.consume();
                    OrderStamp stamp = input1.stamp;
                    input2Map.put(stamp, input1);
                    tryPairAndProduce(stamp, input1Map, input2Map, output);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        });

        return outputBuffer;
    }
}
