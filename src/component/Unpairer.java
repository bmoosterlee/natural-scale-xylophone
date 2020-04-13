package component;

import component.buffer.*;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class Unpairer<K, V, A extends Packet<K>, B extends Packet<V>, Y extends Packet<SimpleImmutableEntry<K, V>>> extends AbstractComponent {

    protected final InputPort<SimpleImmutableEntry<K, V>, Y> input;
    private final OutputPort<K, A> output1;
    private final OutputPort<V, B> output2;

    public Unpairer(BoundedBuffer<SimpleImmutableEntry<K, V>, Y> inputBuffer, SimpleBuffer<K, A> outputBuffer1, SimpleBuffer<V, B> outputBuffer2){
        input = inputBuffer.createInputPort();
        output1 = outputBuffer1.createOutputPort();
        output2 = outputBuffer2.createOutputPort();
    }

    protected void tick(){
        try {
            Y consumed = input.consume();
            SimpleImmutableEntry<K, V> unwrap = consumed.unwrap();
            A result1 = consumed.transform(input -> unwrap.getKey());
            B result2 = consumed.transform(input -> unwrap.getValue());
            output1.produce(result1);
            output2.produce(result2);
        }
        catch(InterruptedException e){
            e.printStackTrace();
        }
    }

    public static <K, V, A extends Packet<K>, B extends Packet<V>, Y extends Packet<SimpleImmutableEntry<K, V>>> SimpleImmutableEntry<SimpleBuffer<K, A>, SimpleBuffer<V, B>> unpair(BoundedBuffer<SimpleImmutableEntry<K, V>, Y> inputBuffer){
        SimpleBuffer<K, A> outputBuffer1 = new SimpleBuffer<>(1, "unpair - output 1");
        SimpleBuffer<V, B> outputBuffer2 = new SimpleBuffer<>(1, "unpair - output 2");
        new TickRunningStrategy(new Unpairer<>(inputBuffer, outputBuffer1, outputBuffer2));
        return new SimpleImmutableEntry<>(outputBuffer1, outputBuffer2);
    }

    @Override
    protected Collection<BoundedBuffer> getInputBuffers() {
        return Collections.singletonList(input.getBuffer());
    }

    @Override
    protected Collection<BoundedBuffer> getOutputBuffers() {
        return Arrays.asList(output1.getBuffer(), output2.getBuffer());
    }

}
