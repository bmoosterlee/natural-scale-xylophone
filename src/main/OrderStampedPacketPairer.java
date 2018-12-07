package main;

import component.buffer.*;
import component.orderer.OrderStamp;
import component.orderer.OrderStampedPacket;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OrderStampedPacketPairer<K, V> extends AbstractComponent {
    private final InputPort<K, OrderStampedPacket<K>> inputPort1;
    private final InputPort<V, OrderStampedPacket<V>> inputPort2;
    private final OutputPort<SimpleImmutableEntry<K, V>, OrderStampedPacket<SimpleImmutableEntry<K, V>>> outputPort;

    private final Map<OrderStamp, OrderStampedPacket<K>> input1Map;
    private final Map<OrderStamp, OrderStampedPacket<V>> input2Map;
    private final Map<OrderStamp, Object> lockMap;

    private boolean index;

    private OrderStampedPacketPairer(BoundedBuffer<K, OrderStampedPacket<K>> inputBuffer1, BoundedBuffer<V, OrderStampedPacket<V>> inputBuffer2, SimpleBuffer<SimpleImmutableEntry<K, V>, OrderStampedPacket<SimpleImmutableEntry<K, V>>> outputBuffer){
        inputPort1 = inputBuffer1.createInputPort();
        inputPort2 = inputBuffer2.createInputPort();
        outputPort = outputBuffer.createOutputPort();

        input1Map = new HashMap<>();
        input2Map = new HashMap<>();
        lockMap = new ConcurrentHashMap<>();
    }

    protected void tick() {
        try {
            if(!inputPort1.isEmpty()){
                tryPairAndProduceType1(inputPort1.consume());
            }
            else if(!inputPort2.isEmpty()){
                tryPairAndProduceType2(inputPort2.consume());
            } else {
                if (index) {
                    tryPairAndProduceType1(inputPort1.consume());
                } else {
                    tryPairAndProduceType2(inputPort2.consume());
                }
                index = !index;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void tryPairAndProduceType1(OrderStampedPacket<K> input) throws InterruptedException {
        OrderStampedPacket<V> counterPart = add(input, input1Map, input2Map);
        if(counterPart!=null){
            pairAndProduce(input, counterPart);
        }
    }

    private void tryPairAndProduceType2(OrderStampedPacket<V> input) throws InterruptedException {
        OrderStampedPacket<K> counterPart = add(input, input2Map, input1Map);
        if(counterPart!=null){
            pairAndProduce(counterPart, input);
        }
    }

    private <P, Q> OrderStampedPacket<Q> add(OrderStampedPacket<P> input, Map<OrderStamp, OrderStampedPacket<P>> myMap, Map<OrderStamp, OrderStampedPacket<Q>> counterMap) {
        synchronized(findOrCreateLock(input.stamp)) {
            if (counterMap.containsKey(input.stamp)) {
                return counterMap.remove(input.stamp);
            } else {
                myMap.put(input.stamp, input);
            }
        }
        return null;
    }

    private Object findOrCreateLock(OrderStamp stamp) {
        Object lock = new Object();
        Object existingLock = lockMap.putIfAbsent(stamp, lock);
        if (existingLock != null) {
            lockMap.remove(stamp);
            return existingLock;
        } else {
            return lock;
        }
    }

    private void pairAndProduce(OrderStampedPacket<K> consumed1, OrderStampedPacket<V> counterPart) throws InterruptedException {
        OrderStampedPacket<SimpleImmutableEntry<K, V>> transform =
                consumed1.transform(input ->
                        new SimpleImmutableEntry<>(
                                input,
                                counterPart.unwrap()));
        outputPort.produce(transform);
    }

    @Override
    protected Collection<BoundedBuffer> getInputBuffers() {
        return Arrays.asList(inputPort1.getBuffer(), inputPort2.getBuffer());
    }

    @Override
    protected Collection<BoundedBuffer> getOutputBuffers() {
        return Collections.singletonList(outputPort.getBuffer());
    }

    public static <K, V> SimpleBuffer<SimpleImmutableEntry<K, V>, OrderStampedPacket<SimpleImmutableEntry<K, V>>> buildComponent(BoundedBuffer<K, OrderStampedPacket<K>> inputBuffer1, BoundedBuffer<V, OrderStampedPacket<V>> inputBuffer2){
        SimpleBuffer<SimpleImmutableEntry<K, V>, OrderStampedPacket<SimpleImmutableEntry<K, V>>> outputBuffer = new SimpleBuffer<>(1, "order stamped packet pairer - output");
        new TickRunningStrategy(new OrderStampedPacketPairer<>(inputBuffer1, inputBuffer2, outputBuffer));
        return outputBuffer;
    }
}
