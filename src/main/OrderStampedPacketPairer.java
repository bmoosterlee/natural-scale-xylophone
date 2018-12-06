package main;

import component.buffer.*;
import component.orderer.OrderStamp;
import component.orderer.OrderStampedPacket;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.*;

public class OrderStampedPacketPairer<K, V> extends AbstractComponent {
    private final InputPort<K, OrderStampedPacket<K>> inputPort1;
    private final InputPort<V, OrderStampedPacket<V>> inputPort2;
    private final OutputPort<SimpleImmutableEntry<K, V>, OrderStampedPacket<SimpleImmutableEntry<K, V>>> outputPort;

    private final Map<OrderStamp, OrderStampedPacket<K>> input1Map;
    private final Map<OrderStamp, OrderStampedPacket<V>> input2Map;
    private final HashMap<OrderStamp, Object> lockMap;

    private boolean index;

    private OrderStampedPacketPairer(BoundedBuffer<K, OrderStampedPacket<K>> inputBuffer1, BoundedBuffer<V, OrderStampedPacket<V>> inputBuffer2, SimpleBuffer<SimpleImmutableEntry<K, V>, OrderStampedPacket<SimpleImmutableEntry<K, V>>> outputBuffer){
        inputPort1 = inputBuffer1.createInputPort();
        inputPort2 = inputBuffer2.createInputPort();
        outputPort = outputBuffer.createOutputPort();

        input1Map = new HashMap<>();
        input2Map = new HashMap<>();
        lockMap = new HashMap<>();
    }

    protected void tick() {
        try {
            if(!inputPort1.isEmpty()){
                addType1(inputPort1.consume());
            }
            else if(!inputPort2.isEmpty()){
                addType2(inputPort2.consume());
            } else {
                if (index) {
                    addType1(inputPort1.consume());
                } else {
                    addType2(inputPort2.consume());
                }
                index = !index;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void addType1(OrderStampedPacket<K> input1) throws InterruptedException {
        synchronized(findOrCreateLock(input1.stamp)) {
            if (input2Map.containsKey(input1.stamp)) {
                OrderStampedPacket<V> counterPart = input2Map.remove(input1.stamp);
                pairAndProduce(input1, counterPart);
            } else {
                input1Map.put(input1.stamp, input1);
            }
        }
    }

    private void addType2(OrderStampedPacket<V> input2) throws InterruptedException {
        synchronized (findOrCreateLock(input2.stamp)) {
            if (input1Map.containsKey(input2.stamp)) {
                OrderStampedPacket<K> counterPart = input1Map.remove(input2.stamp);
                pairAndProduce(counterPart, input2);
            } else {
                input2Map.put(input2.stamp, input2);
            }
        }
    }

    private Object findOrCreateLock(OrderStamp stamp) {
        synchronized (lockMap){
            if (!lockMap.containsKey(stamp)) {
                Object lock = new Object();
                lockMap.put(stamp, lock);
                return lock;
            }
        }
        return lockMap.remove(stamp);
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
