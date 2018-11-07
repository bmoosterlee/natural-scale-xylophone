package main;

import java.util.AbstractMap;

public class PipeComponent<K, V> extends Component {

    protected final InputPort<K> input;
    protected final OutputPort<V> output;
    protected final CallableWithArguments<K, V> method;

    public PipeComponent(BoundedBuffer<K> inputBuffer, BoundedBuffer<V> outputBuffer, CallableWithArguments<K, V> method){
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
        BoundedBuffer<K> inputBuffer = new BoundedBuffer<>(capacity, name + " input");
        BoundedBuffer<V> outputBuffer = new BoundedBuffer<>(capacity, name + " output");
        new PipeComponent<>(inputBuffer, outputBuffer, method);
        return new AbstractMap.SimpleImmutableEntry<>(inputBuffer, outputBuffer);
    }

    public static <K, V> AbstractMap.SimpleImmutableEntry<OutputPort<K>, InputPort<V>> methodToComponentPorts(CallableWithArguments<K, V> method, int capacity, String name){
        AbstractMap.SimpleImmutableEntry<BoundedBuffer<K>, BoundedBuffer<V>> buffers = PipeComponent.methodToComponentBuffers(method, capacity, name);
        BoundedBuffer<K> inputBuffer = buffers.getKey();
        BoundedBuffer<V> outputBuffer = buffers.getValue();
        OutputPort<K> outputPort = new OutputPort<>(inputBuffer);
        InputPort<V> inputPort = new InputPort<>(outputBuffer);
        return new AbstractMap.SimpleImmutableEntry<>(outputPort, inputPort);
    }
}
