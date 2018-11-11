package component;

import java.util.AbstractMap.SimpleImmutableEntry;

public class Pairer<K, V> extends TickablePipeComponent<K, SimpleImmutableEntry<K, V>> {

    public Pairer(BoundedBuffer<K> inputBuffer1, BoundedBuffer<V> inputBuffer2, BoundedBuffer<SimpleImmutableEntry<K, V>> outputBuffer){
        super(inputBuffer1, outputBuffer, build(inputBuffer2));

    }

    public static <K, V> CallableWithArguments<K, SimpleImmutableEntry<K, V>> build(BoundedBuffer<V> inputBuffer2){
        return new CallableWithArguments<>() {
            private final InputPort<V> inputPort2;

            {
                inputPort2 = new InputPort<>(inputBuffer2);
            }

    public static <K, V> BoundedBuffer<SimpleImmutableEntry<K, V>> PairerWithOutputBuffer(BoundedBuffer<K> inputBuffer1, BoundedBuffer<V> inputBuffer2, int capacity, String name){
        BoundedBuffer<SimpleImmutableEntry<K,V>> outputBuffer = new BoundedBuffer<>(capacity, name);
        new Pairer<>(inputBuffer1, inputBuffer2, outputBuffer);
        return outputBuffer;
    }
}
