package component.utilities;

import component.buffer.*;

import java.util.AbstractMap;

public class TickablePipeComponent<K, V> extends PipeComponent<K, V> {

    private final MyTickable tickable = new MyTickable();

    public TickablePipeComponent(BoundedBuffer<K> inputBuffer, BoundedBuffer<V> outputBuffer, CallableWithArguments<K, V> method){
        super(inputBuffer, outputBuffer, method);

        start();
    }

    private class MyTickable extends Tickable {

        @Override
        protected void tick() {
            TickablePipeComponent.this.tick();
        }

    }

    protected void start() {
        tickable.start();
    }

    public static <K, V> AbstractMap.SimpleImmutableEntry<BoundedBuffer<K>, BoundedBuffer<V>> methodToComponentBuffers(CallableWithArguments<K, V> method, int capacity, String name){
        BoundedBuffer<K> inputBuffer = new SimpleBuffer<>(capacity, name + " input");
        SimpleBuffer<V> outputBuffer = new SimpleBuffer<>(capacity, name + " output");
        new TickablePipeComponent<>(inputBuffer, outputBuffer, method);
        return new AbstractMap.SimpleImmutableEntry<>(inputBuffer, outputBuffer);
    }

    public static <K, V> AbstractMap.SimpleImmutableEntry<OutputPort<K>, InputPort<V>> methodToComponentPorts(CallableWithArguments<K, V> method, int capacity, String name){
        AbstractMap.SimpleImmutableEntry<BoundedBuffer<K>, BoundedBuffer<V>> buffers = TickablePipeComponent.methodToComponentBuffers(method, capacity, name);
        BoundedBuffer<K> inputBuffer = buffers.getKey();
        BoundedBuffer<V> outputBuffer = buffers.getValue();
        OutputPort<K> outputPort = new OutputPort<>(inputBuffer);
        InputPort<V> inputPort = new InputPort<>(outputBuffer);
        return new AbstractMap.SimpleImmutableEntry<>(outputPort, inputPort);
    }
}
