package component;

import component.buffer.*;

import java.util.*;
import java.util.AbstractMap.SimpleImmutableEntry;

public class Mapper<K, V> extends AbstractComponent<SimpleImmutableEntry<K, V>, V> {
    private InputPort<SimpleImmutableEntry<K, V>> inputPort;
    private final Map<K, OutputPort<V>> outputPortMap;

    private Mapper(BoundedBuffer<SimpleImmutableEntry<K, V>> inputBuffer, Map<K, BoundedBuffer<V>> outputBufferMap) {
        inputPort = inputBuffer.createInputPort();
        outputPortMap = new HashMap<>();
        for (K index : outputBufferMap.keySet()) {
            outputPortMap.put(index, outputBufferMap.get(index).createOutputPort());
        }
    }

    @Override
    protected void tick() {
        try {
            SimpleImmutableEntry<K, V> consumed = inputPort.consume();
            outputPortMap.get(consumed.getKey()).produce(consumed.getValue());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Collection<BoundedBuffer<SimpleImmutableEntry<K, V>>> getInputBuffers() {
        return Collections.singleton(inputPort.getBuffer());
    }

    @Override
    protected Collection<BoundedBuffer<V>> getOutputBuffers() {
        HashSet<BoundedBuffer<V>> outputBuffers = new HashSet<>();
        for(OutputPort<V> outputPort : outputPortMap.values()){
            outputBuffers.add(outputPort.getBuffer());
        }
        return outputBuffers;
    }

    public static <K, V> Map<K, BoundedBuffer<V>> buildComponent(BoundedBuffer<SimpleImmutableEntry<K, V>> inputBuffer, Collection<K> range){
        Map<K, BoundedBuffer<V>> outputBufferMap = new HashMap<>();
        for (K index : range) {
            outputBufferMap.put(index, new SimpleBuffer<>(1,"map bucket"));
        }

        new TickRunningStrategy(new Mapper<>(inputBuffer, outputBufferMap));

        return outputBufferMap;
    }
}
