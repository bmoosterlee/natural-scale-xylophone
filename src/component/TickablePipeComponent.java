package component;

import java.util.AbstractMap;

public class TickablePipeComponent<K, V> extends Tickable {

    protected final InputPort<K> input;
    protected final OutputPort<V> output;
    private final CallableWithArguments<K, V> method;

    public TickablePipeComponent(BufferInterface<K> inputBuffer, BufferInterface<V> outputBuffer, CallableWithArguments<K, V> method){
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

    public static <K, V> AbstractMap.SimpleImmutableEntry<BufferInterface<K>, BufferInterface<V>> methodToComponentBuffers(CallableWithArguments<K, V> method, int capacity, String name){
        BufferInterface<K> inputBuffer = new SimpleBuffer<>(capacity, name + " input");
        SimpleBuffer<V> outputBuffer = new SimpleBuffer<>(capacity, name + " output");
        new TickablePipeComponent<>(inputBuffer, outputBuffer, method);
        return new AbstractMap.SimpleImmutableEntry<>(inputBuffer, outputBuffer);
    }

    public static <K, V> AbstractMap.SimpleImmutableEntry<OutputPort<K>, InputPort<V>> methodToComponentPorts(CallableWithArguments<K, V> method, int capacity, String name){
        AbstractMap.SimpleImmutableEntry<BufferInterface<K>, BufferInterface<V>> buffers = TickablePipeComponent.methodToComponentBuffers(method, capacity, name);
        BufferInterface<K> inputBuffer = buffers.getKey();
        BufferInterface<V> outputBuffer = buffers.getValue();
        OutputPort<K> outputPort = new OutputPort<>(inputBuffer);
        InputPort<V> inputPort = new InputPort<>(outputBuffer);
        return new AbstractMap.SimpleImmutableEntry<>(outputPort, inputPort);
    }

    public static <K, V> BufferInterface<V> methodToComponentWithOutputBuffer(BufferInterface<K> inputBuffer, CallableWithArguments<K,V> method, int capacity, String name) {
        SimpleBuffer<V> outputBuffer = new SimpleBuffer<>(capacity, name);
        new TickablePipeComponent<>(inputBuffer, outputBuffer, method);
        return outputBuffer;
    }

}
