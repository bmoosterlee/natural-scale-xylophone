package component;

import component.buffer.*;

import java.util.*;

public class Unmapper<K, V, A extends Packet<K>, B extends Packet<V>> extends AbstractComponent<V, Map<K, V>, B, SimplePacket<Map<K, V>>> {
    private final Map<K, InputPort<V, B>> inputPortMap;
    private final OutputPort<Map<K, V>, SimplePacket<Map<K, V>>> outputPort;

    private Unmapper(Map<K, BoundedBuffer<V, B>> inputBufferMap, SimpleBuffer<Map<K, V>, SimplePacket<Map<K, V>>> outputBuffer) {
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
                map.put(index, inputPortMap.get(index).consume().unwrap());
            }
            outputPort.produce(new SimplePacket<>(map));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Collection<BoundedBuffer<V, B>> getInputBuffers() {
        HashSet<BoundedBuffer<V, B>> inputBuffers = new HashSet<>();
        for(InputPort<V, B> inputPort : inputPortMap.values()){
            inputBuffers.add(inputPort.getBuffer());
        }
        return inputBuffers;
    }

    @Override
    protected Collection<BoundedBuffer<Map<K, V>, SimplePacket<Map<K, V>>>> getOutputBuffers() {
        return Collections.singleton(outputPort.getBuffer());
    }

    public static <K, V, B extends Packet<V>> SimpleBuffer<Map<K, V>, SimplePacket<Map<K, V>>> buildComponent(Map<K, BoundedBuffer<V, B>> inputBufferMap) {
        SimpleBuffer<Map<K, V>, SimplePacket<Map<K, V>>> outputBuffer = new SimpleBuffer<>(1, "unmap");

        new TickRunningStrategy(new Unmapper<>(inputBufferMap, outputBuffer));

        return outputBuffer;
    }
}
