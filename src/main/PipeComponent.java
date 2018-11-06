package main;

public abstract class PipeComponent<K, V> extends Component {

    protected final InputPort<K> input;
    protected final OutputPort<V> output;

    public PipeComponent(BoundedBuffer<K> inputBuffer, BoundedBuffer<V> outputBuffer){
        input = new InputPort<>(inputBuffer);
        output = new OutputPort<>(outputBuffer);
    }

    public static <K, V> PipeComponent<K, V> methodToComponent(CallableWithArguments<K, V> method, BoundedBuffer<K> inputBuffer, BoundedBuffer<V> outputBuffer){
        return new PipeComponent<>(inputBuffer, outputBuffer) {
            @Override
            protected void tick() {
                try {
                    K consumed = input.consume();
                    V result = method.call(consumed);
                    output.produce(result);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
    }
}
