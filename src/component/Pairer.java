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

            private SimpleImmutableEntry<K, V> pair(K consumed1) {
                try {
                    V consumed2 = inputPort2.consume();
                    return new SimpleImmutableEntry<>(consumed1, consumed2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public SimpleImmutableEntry<K, V> call(K input) {
                return pair(input);
            }
        };
    }

}
