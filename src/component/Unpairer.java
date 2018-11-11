package component;

import java.util.AbstractMap.SimpleImmutableEntry;

public class Unpairer<K, V> extends TickablePipeComponent<SimpleImmutableEntry<K, V>, V> {

    public Unpairer(BufferInterface<SimpleImmutableEntry<K,V>> inputBuffer, SimpleBuffer<K> outputBuffer1, SimpleBuffer<V> outputBuffer2){
        super(inputBuffer, outputBuffer2, build(outputBuffer1));
    }


    protected static <K, V> CallableWithArguments<SimpleImmutableEntry<K, V>, V> build(SimpleBuffer<K> outputBuffer1){
        return new CallableWithArguments<>() {
            private final OutputPort<K> outputPort1;

            {
                outputPort1 = new OutputPort<>(outputBuffer1);
            }

            private V unpair(SimpleImmutableEntry<K, V> consumed) {
                try {
                    K result1 = consumed.getKey();
                    V result2 = consumed.getValue();
                    outputPort1.produce(result1);
                    return result2;
                }
                catch(InterruptedException e){
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public V call(SimpleImmutableEntry<K, V> input) {
                return unpair(input);
            }
        };
    }


}
