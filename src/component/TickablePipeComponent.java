package component;

import java.util.AbstractMap;

public class TickablePipeComponent<K, V> extends Tickable {

    protected final InputPort<K> input;
    protected final OutputPort<V> output;
    private final CallableWithArguments<K, V> method;

    public TickablePipeComponent(BoundedBuffer<K> inputBuffer, BoundedBuffer<V> outputBuffer, CallableWithArguments<K, V> method){
        this.method = method;

        input = new InputPort<>(inputBuffer);
        output = new OutputPort<>(outputBuffer);

        start();
    }

    @Override
    protected void tick() {
        try {
            K consumed = input.consume();
            V result = method.call(consumed);
            output.produce(result);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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

    public static <K, V> BoundedBuffer<V> methodToComponentWithOutputBuffer(BoundedBuffer<K> inputBuffer, CallableWithArguments<K,V> method, int capacity, String name) {
        SimpleBuffer<V> outputBuffer = new SimpleBuffer<>(capacity, name);
        new TickablePipeComponent<>(inputBuffer, outputBuffer, method);
        return outputBuffer;
    }

}
