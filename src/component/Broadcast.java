package component;

import component.buffer.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;

public class Broadcast<K, T extends Packet<K>> extends AbstractComponent<K, K, T, T> {

    private final InputPort<K, T> input;
    private final Collection<OutputPort<K, T>> outputs;

    public Broadcast(SimpleBuffer<K, T> inputBuffer, Collection<SimpleBuffer<K, T>> outputBuffers) {
        input = inputBuffer.createInputPort();

        outputs = new HashSet<>();
        for(SimpleBuffer<K, T> buffer : outputBuffers){
            outputs.add(new OutputPort<>(buffer));
        }
    }

    protected void tick() {
        try {
            T item = input.consume();
            for(OutputPort<K, T> output : outputs){
                output.produce(item);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static <K, T extends Packet<K>> Collection<SimpleBuffer<K, T>> broadcast(SimpleBuffer<K, T> inputBuffer, int consumers){
        return broadcast(inputBuffer, consumers, "unnamed broadcast");
    }

    public static <K, T extends Packet<K>> Collection<SimpleBuffer<K, T>> broadcast(SimpleBuffer<K, T> inputBuffer, int consumers, String name) {
        Collection<SimpleBuffer<K, T>> outputBuffers = new LinkedList<>();
        for(int i = 0; i<consumers; i++){
            outputBuffers.add(new SimpleBuffer<>(1, name + " broadcast: buffer " + String.valueOf(i)));
        }
        new TickRunningStrategy(new Broadcast<>(inputBuffer, outputBuffers));
        return outputBuffers;
    }

    @Override
    protected Collection<BoundedBuffer<K, T>> getInputBuffers() {
        return Collections.singleton(input.getBuffer());
    }

    @Override
    protected Collection<BoundedBuffer<K, T>> getOutputBuffers() {
        Collection<BoundedBuffer<K, T>> outputBuffers = new HashSet<>();
        for(OutputPort<K, T> outputPort : outputs){
            outputBuffers.add(outputPort.getBuffer());
        }
        return outputBuffers;
    }
}
