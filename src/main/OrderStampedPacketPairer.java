package main;

import component.buffer.*;
import component.orderer.OrderStamp;
import component.orderer.OrderStampedPacket;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OrderStampedPacketPairer{

    private static <P, Q> OrderStampedPacket<Q> add(OrderStampedPacket<P> input, Map<OrderStamp, OrderStampedPacket<P>> myMap, Map<OrderStamp, OrderStampedPacket<Q>> counterMap, Map<OrderStamp, Object> lockMap) {
        synchronized(findOrCreateLock(input.stamp, lockMap)) {
            if (counterMap.containsKey(input.stamp)) {
                lockMap.remove(input.stamp);
                return counterMap.remove(input.stamp);
            } else {
                myMap.put(input.stamp, input);
            }
        }
        return null;
    }

    private static Object findOrCreateLock(OrderStamp stamp, Map<OrderStamp, Object> lockMap) {
        return lockMap.computeIfAbsent(stamp, orderStamp -> new Object());
    }

    private static <K, V> void pairAndProduce(OrderStampedPacket<K> consumed1, OrderStampedPacket<V> counterPart, OutputPort<SimpleImmutableEntry<K, V>, OrderStampedPacket<SimpleImmutableEntry<K, V>>> outputPort) throws InterruptedException {
        OrderStampedPacket<SimpleImmutableEntry<K, V>> transform =
                consumed1.transform(input ->
                        new SimpleImmutableEntry<>(
                                input,
                                counterPart.unwrap()));
        outputPort.produce(transform);
    }

    public static <K, V> SimpleBuffer<SimpleImmutableEntry<K, V>, OrderStampedPacket<SimpleImmutableEntry<K, V>>> buildComponent(BoundedBuffer<K, OrderStampedPacket<K>> inputBuffer1, BoundedBuffer<V, OrderStampedPacket<V>> inputBuffer2){
        SimpleBuffer<SimpleImmutableEntry<K, V>, OrderStampedPacket<SimpleImmutableEntry<K, V>>> outputBuffer = new SimpleBuffer<>(1, "order stamped packet pairer - output");

        final Map<OrderStamp, OrderStampedPacket<K>> input1Map = new ConcurrentHashMap<>();
        final Map<OrderStamp, OrderStampedPacket<V>> input2Map = new ConcurrentHashMap<>();
        final Map<OrderStamp, Object> lockMap = new ConcurrentHashMap<>();

        new TickRunningStrategy(new AbstractPipeComponent<>(inputBuffer1.createInputPort(), outputBuffer.createOutputPort()) {
            @Override
            protected void tick() {
                try {
                    tryPairAndProduceType1(input.consume());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            private void tryPairAndProduceType1(OrderStampedPacket<K> input) throws InterruptedException {
                OrderStampedPacket<V> counterPart = add(input, input1Map, input2Map, lockMap);
                if(counterPart!=null){
                    pairAndProduce(input, counterPart, output);
                }
            }
        });
        new TickRunningStrategy(new AbstractPipeComponent<>(inputBuffer2.createInputPort(), outputBuffer.createOutputPort()) {
            @Override
            protected void tick() {
                try {
                    tryPairAndProduceType2(input.consume());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            private void tryPairAndProduceType2(OrderStampedPacket<V> input) throws InterruptedException {
                OrderStampedPacket<K> counterPart = add(input, input2Map, input1Map, lockMap);
                if(counterPart!=null){
                    pairAndProduce(counterPart, input, output);
                }
            }
        });

        return outputBuffer;
    }
}
