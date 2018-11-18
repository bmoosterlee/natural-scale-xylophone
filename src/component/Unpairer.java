package component;

import component.buffer.*;

import java.util.AbstractMap.SimpleImmutableEntry;

public class Unpairer<K, V> {

    protected final InputPort<SimpleImmutableEntry<K, V>> input;
    private final OutputPort<K> output1;
    private final OutputPort<V> output2;

    TickRunner tickRunner = new MyTickRunner();

    public Unpairer(BoundedBuffer<SimpleImmutableEntry<K,V>> inputBuffer, SimpleBuffer<K> outputBuffer1, SimpleBuffer<V> outputBuffer2){
        input = inputBuffer.createInputPort();
        output1 = outputBuffer1.createOutputPort();
        output2 = outputBuffer2.createOutputPort();

        tickRunner.start();
    }

    private class MyTickRunner extends SimpleTickRunner {
        @Override
        protected void tick() {
            Unpairer.this.tick();
        }
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

}
