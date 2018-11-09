package component;

import java.util.AbstractMap.SimpleImmutableEntry;

public class Unpairer<K, V> extends Tickable {

    private final InputPort<SimpleImmutableEntry<K, V>> inputPort;
    private final OutputPort<K> outputPort1;
    private final OutputPort<V> outputPort2;

    public Unpairer(BoundedBuffer<SimpleImmutableEntry<K, V>> inputBuffer, BoundedBuffer<K> outputBuffer1, BoundedBuffer<V> outputBuffer2){
        inputPort = new InputPort<>(inputBuffer);
        outputPort1 = new OutputPort<>(outputBuffer1);
        outputPort2 = new OutputPort<>(outputBuffer2);

        start();
    }

    @Override
    protected void tick() {
        try {
            SimpleImmutableEntry<K, V> consumed = inputPort.consume();
            K result1 = consumed.getKey();
            V result2 = consumed.getValue();
            outputPort1.produce(result1);
            outputPort2.produce(result2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static <K, V> BoundedBuffer<SimpleImmutableEntry<K, V>> UnpairerWithInputBuffer(BoundedBuffer<K> outputBuffer1, BoundedBuffer<V> outputBuffer2, int capacity, String name){
        BoundedBuffer<SimpleImmutableEntry<K,V>> inputBuffer = new BoundedBuffer<>(capacity, name);
        new Unpairer<>(inputBuffer, outputBuffer1, outputBuffer2);
        return inputBuffer;
    }
}
