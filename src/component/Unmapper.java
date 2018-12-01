package component;

import component.buffer.*;
import spectrum.buckets.BuffersToBuckets;

import java.util.*;

public class Unmapper<V, K> extends AbstractComponent<V, Map<K, V>> {
    private final Map<K, InputPort<V>> inputPortMap;
    private final OutputPort<Map<K, V>> outputPort;

    private Unmapper(Map<K, BoundedBuffer<V>> inputBufferMap, SimpleBuffer<Map<K, V>> outputBuffer) {
        inputPortMap = new HashMap<>();
        for (K index : inputBufferMap.keySet()) {
            inputPortMap.put(index, inputBufferMap.get(index).createInputPort());
        }
        outputPort = outputBuffer.createOutputPort();
    }

    @Override
    protected void tick() {
        try {
            Map<K, V> map = new HashMap<>();
            for (K index : inputPortMap.keySet()) {
                map.put(index, inputPortMap.get(index).consume());
            }
            outputPort.produce(map);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Collection<BoundedBuffer<V>> getInputBuffers() {
        HashSet<BoundedBuffer<V>> inputBuffers = new HashSet<>();
        for(InputPort<V> inputPort : inputPortMap.values()){
            inputBuffers.add(inputPort.getBuffer());
        }
        return inputBuffers;
    }

    @Override
    protected Collection<BoundedBuffer<Map<K, V>>> getOutputBuffers() {
        return Collections.singleton(outputPort.getBuffer());
    }

    public static <K, V> SimpleBuffer<Map<K, V>> buildComponent(Map<K, BoundedBuffer<V>> inputBufferMap) {
        SimpleBuffer<Map<K, V>> outputBuffer = new SimpleBuffer<>(1, "unmap");

        new TickRunningStrategy(new Unmapper<>(inputBufferMap, outputBuffer));

        return outputBuffer;
    }
}
