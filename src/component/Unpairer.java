package component;

import component.buffer.*;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class Unpairer<K, V> extends AbstractComponent {

    protected final InputPort<SimpleImmutableEntry<K, V>> input;
    private final OutputPort<K> output1;
    private final OutputPort<V> output2;

    public Unpairer(BoundedBuffer<SimpleImmutableEntry<K,V>> inputBuffer, SimpleBuffer<K> outputBuffer1, SimpleBuffer<V> outputBuffer2){
        input = inputBuffer.createInputPort();
        output1 = outputBuffer1.createOutputPort();
        output2 = outputBuffer2.createOutputPort();
    }

    protected void tick(){
        try {
            SimpleImmutableEntry<K, V> consumed = input.consume();
            K result1 = consumed.getKey();
            V result2 = consumed.getValue();
            output1.produce(result1);
            output2.produce(result2);
        }
        catch(InterruptedException e){
            e.printStackTrace();
        }
    }

    public static <K, V> SimpleImmutableEntry<SimpleBuffer<K>, SimpleBuffer<V>> unpair(BoundedBuffer<SimpleImmutableEntry<K, V>> inputBuffer){
        SimpleBuffer<K> outputBuffer1 = new SimpleBuffer<>(1, "unpair - output 1");
        SimpleBuffer<V> outputBuffer2 = new SimpleBuffer<>(1, "unpair - output 2");
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
