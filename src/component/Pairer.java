package component;

import java.util.AbstractMap.SimpleImmutableEntry;

public class Pairer<K, V> extends Tickable {

    private final InputPort<K> inputPort1;
    private final InputPort<V> inputPort2;
    private final OutputPort<SimpleImmutableEntry<K, V>> outputPort;

    public Pairer(BoundedBuffer<K> inputBuffer1, BoundedBuffer<V> inputBuffer2, BoundedBuffer<SimpleImmutableEntry<K, V>> outputBuffer){
        inputPort1 = new InputPort<>(inputBuffer1);
        inputPort2 = new InputPort<>(inputBuffer2);
        outputPort = new OutputPort<>(outputBuffer);

        start();
    }

    @Override
    protected void tick() {
        try {
            K consumed1 = inputPort1.consume();
            V consumed2 = inputPort2.consume();
            SimpleImmutableEntry<K, V> result = new SimpleImmutableEntry<>(consumed1, consumed2);
            outputPort.produce(result);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static <K, V> BoundedBuffer<SimpleImmutableEntry<K, V>> PairerWithOutputBuffer(BoundedBuffer<K> inputBuffer1, BoundedBuffer<V> inputBuffer2, int capacity, String name){
        BoundedBuffer<SimpleImmutableEntry<K,V>> outputBuffer = new BoundedBuffer<>(capacity, name);
        new Pairer<>(inputBuffer1, inputBuffer2, outputBuffer);
        return outputBuffer;
    }
}
