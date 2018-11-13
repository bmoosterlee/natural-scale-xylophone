package component.buffer;

import java.util.AbstractMap;

public class RunningPipeComponent<K, V> extends PipeComponent<K, V> {

    private final MyTickRunner tickRunner = new MyTickRunner();

    public RunningPipeComponent(BoundedBuffer<K> inputBuffer, BoundedBuffer<V> outputBuffer, CallableWithArguments<K, V> method){
        super(inputBuffer, outputBuffer, method);

        start();
    }

    private class MyTickRunner extends TickRunner {

        @Override
        protected void tick() {
            RunningPipeComponent.this.tick();
        }

    }

    protected void start() {
        tickRunner.start();
    }

    public static <K, V> AbstractMap.SimpleImmutableEntry<BoundedBuffer<K>, BoundedBuffer<V>> methodToComponentBuffers(CallableWithArguments<K, V> method, int capacity, String name){
        BoundedBuffer<K> inputBuffer = new SimpleBuffer<>(capacity, name + " input");
        SimpleBuffer<V> outputBuffer = new SimpleBuffer<>(capacity, name + " output");
        new RunningPipeComponent<>(inputBuffer, outputBuffer, method);
        return new AbstractMap.SimpleImmutableEntry<>(inputBuffer, outputBuffer);
    }

    public static <K, V> AbstractMap.SimpleImmutableEntry<OutputPort<K>, InputPort<V>> methodToComponentPorts(CallableWithArguments<K, V> method, int capacity, String name){
        AbstractMap.SimpleImmutableEntry<BoundedBuffer<K>, BoundedBuffer<V>> buffers = RunningPipeComponent.methodToComponentBuffers(method, capacity, name);
        BoundedBuffer<K> inputBuffer = buffers.getKey();
        BoundedBuffer<V> outputBuffer = buffers.getValue();
        OutputPort<K> outputPort = new OutputPort<>(inputBuffer);
        InputPort<V> inputPort = outputBuffer.createInputPort();
        return new AbstractMap.SimpleImmutableEntry<>(outputPort, inputPort);
    }
}
