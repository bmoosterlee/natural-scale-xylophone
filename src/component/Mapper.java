package component;

import component.buffer.*;

import java.util.*;
import java.util.AbstractMap.SimpleImmutableEntry;

public class Mapper<K, V, B extends Packet<V>, Y extends Packet<SimpleImmutableEntry<K, V>>> extends AbstractComponent<SimpleImmutableEntry<K, V>, V, Y, B> {
    private InputPort<SimpleImmutableEntry<K, V>, Y> inputPort;
    private final Map<K, OutputPort<V, B>> outputPortMap;

    private Mapper(BoundedBuffer<SimpleImmutableEntry<K, V>, Y> inputBuffer, Map<K, BoundedBuffer<V, B>> outputBufferMap) {
        inputPort = inputBuffer.createInputPort();
        outputPortMap = new HashMap<>();
        for (K index : outputBufferMap.keySet()) {
            outputPortMap.put(index, outputBufferMap.get(index).createOutputPort());
        }
    }

    @Override
    protected void tick() {
        try {
            Y consumed = inputPort.consume();
            outputPortMap.get(consumed.unwrap().getKey()).produce(consumed.transform(input -> input.getValue()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static <I, K, V, A extends Packet<K>, B extends Packet<V>> Map<I, BoundedBuffer<V, B>> forEach(Map<I, BoundedBuffer<K, A>> input, Map<I, PipeCallable<K, V>> methods) {
        Map<I, BoundedBuffer<V, B>> output = new HashMap<>();
        for (I index : input.keySet()) {
            output.put(index, input.get(index).performMethod(methods.get(index), "buffer map - method application"));
        }
        return output;
    }

    public static <I, K, V, A extends Packet<K>, B extends Packet<V>> Map<I, BoundedBuffer<V, B>> forEach(Map<I, BoundedBuffer<K, A>> input, PipeCallable<K, V> method) {
        Map<I, BoundedBuffer<V, B>> output = new HashMap<>();
        for (I index : input.keySet()) {
            output.put(index, input.get(index).performMethod(method, "buffer map - single method application"));
        }
        return output;
    }

    @Override
    protected Collection<BoundedBuffer<SimpleImmutableEntry<K, V>, Y>> getInputBuffers() {
        return Collections.singleton(inputPort.getBuffer());
    }

    @Override
    protected Collection<BoundedBuffer<V, B>> getOutputBuffers() {
        HashSet<BoundedBuffer<V, B>> outputBuffers = new HashSet<>();
        for(OutputPort<V, B> outputPort : outputPortMap.values()){
            outputBuffers.add(outputPort.getBuffer());
        }
        return outputBuffers;
    }

    public static <K, V, B extends Packet<V>, Y extends Packet<SimpleImmutableEntry<K, V>>> Map<K, BoundedBuffer<V, B>> buildComponent(BoundedBuffer<SimpleImmutableEntry<K, V>, Y> inputBuffer, Collection<K> range){
        Map<K, BoundedBuffer<V, B>> outputBufferMap = new HashMap<>();
        for (K index : range) {
            outputBufferMap.put(index, new SimpleBuffer<>(1,"map bucket"));
        }

        new TickRunningStrategy(new Mapper<>(inputBuffer, outputBufferMap));

        return outputBufferMap;
    }
}
