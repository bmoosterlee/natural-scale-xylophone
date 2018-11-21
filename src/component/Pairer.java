package component;

import component.buffer.*;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class Pairer<K, V> extends AbstractComponent {

    private final InputPort<K> inputPort1;
    private final InputPort<V> inputPort2;
    private final OutputPort<SimpleImmutableEntry<K, V>> outputPort;

    public Pairer(BoundedBuffer<K> inputBuffer1, BoundedBuffer<V> inputBuffer2, SimpleBuffer<SimpleImmutableEntry<K, V>> outputBuffer){
        inputPort1 = inputBuffer1.createInputPort();
        inputPort2 = inputBuffer2.createInputPort();
        outputPort = outputBuffer.createOutputPort();
    }

    protected void tick() {
        try {
            K consumed1 = inputPort1.consume();
            V consumed2 = inputPort2.consume();
            outputPort.produce(new SimpleImmutableEntry<>(consumed1, consumed2));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static <K, V> SimpleBuffer<SimpleImmutableEntry<K, V>> pair(BoundedBuffer<K> inputBuffer1, BoundedBuffer<V> inputBuffer2){
        SimpleBuffer<SimpleImmutableEntry<K, V>> outputBuffer = new SimpleBuffer<>(1, "pair");
        new TickRunningStrategy(new Pairer<>(inputBuffer1, inputBuffer2, outputBuffer));
        return outputBuffer;
    }

    @Override
    protected Collection<BoundedBuffer> getInputBuffers() {
        return Arrays.asList(inputPort1.getBuffer(), inputPort2.getBuffer());
    }

    @Override
    protected Collection<BoundedBuffer> getOutputBuffers() {
        return Collections.singletonList(outputPort.getBuffer());
    }

}
