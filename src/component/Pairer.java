package component;

import component.buffer.*;
import component.buffer.TickRunner;

import java.util.AbstractMap.SimpleImmutableEntry;

public class Pairer<K, V> {

    private final InputPort<K> inputPort1;
    private final InputPort<V> inputPort2;
    private final OutputPort<SimpleImmutableEntry<K, V>> outputPort;
    private final TickRunner tickRunner = new MyTickRunner();

    public Pairer(BoundedBuffer<K> inputBuffer1, BoundedBuffer<V> inputBuffer2, SimpleBuffer<SimpleImmutableEntry<K, V>> outputBuffer){
        inputPort1 = inputBuffer1.createInputPort();
        inputPort2 = inputBuffer2.createInputPort();
        outputPort = outputBuffer.createOutputPort();

        tickRunner.start();
    }

    private class MyTickRunner extends TickRunner {
        @Override
        protected void tick() {
            Pairer.this.tick();
        }
    }

    private void tick() {
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
        new Pairer<>(inputBuffer1, inputBuffer2, outputBuffer);
        return outputBuffer;
    }

}
