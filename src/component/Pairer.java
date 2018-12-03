package component;

import component.buffer.*;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class Pairer<K, V, A extends Packet<K>, B extends Packet<V>, Y extends Packet<SimpleImmutableEntry<K, V>>> extends AbstractComponent {

    private final InputPort<K, A> inputPort1;
    private final InputPort<V, B> inputPort2;
    private final OutputPort<SimpleImmutableEntry<K, V>, Y> outputPort;

    public Pairer(BoundedBuffer<K, A> inputBuffer1, BoundedBuffer<V, B> inputBuffer2, SimpleBuffer<SimpleImmutableEntry<K, V>, Y> outputBuffer){
        inputPort1 = inputBuffer1.createInputPort();
        inputPort2 = inputBuffer2.createInputPort();
        outputPort = outputBuffer.createOutputPort();
    }

    protected void tick() {
        try {
            A consumed1 = inputPort1.consume();
            B consumed2 = inputPort2.consume();

            Y transform = consumed1.transform(input -> new SimpleImmutableEntry<>(input, consumed2.unwrap()));

            outputPort.produce(transform);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static <K, V, A extends Packet<K>, B extends Packet<V>, Y extends Packet<SimpleImmutableEntry<K, V>>> SimpleBuffer<SimpleImmutableEntry<K, V>, Y> pair(BoundedBuffer<K, A> inputBuffer1, BoundedBuffer<V, B> inputBuffer2){
        return pair(inputBuffer1, inputBuffer2, "pair");
    }

    public static <K, V, A extends Packet<K>, B extends Packet<V>, Y extends Packet<SimpleImmutableEntry<K, V>>> SimpleBuffer<SimpleImmutableEntry<K, V>, Y> pair(BoundedBuffer<K, A> inputBuffer1, BoundedBuffer<V, B> inputBuffer2, String name){
        SimpleBuffer<SimpleImmutableEntry<K, V>, Y> outputBuffer = new SimpleBuffer<>(1, name);
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

    @Override
    public Boolean isParallelisable(){
        return false;
    }
}
