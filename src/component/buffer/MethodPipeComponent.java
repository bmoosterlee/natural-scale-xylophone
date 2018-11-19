package component.buffer;

public class MethodPipeComponent<K, V> extends AbstractPipeComponent<K, V> {
    protected final CallableWithArguments<K, V> method;

    public MethodPipeComponent(SimpleBuffer<K> inputBuffer, BoundedBuffer<V> outputBuffer, CallableWithArguments<K, V> method) {
        super(inputBuffer.createInputPort(), new OutputPort<>(outputBuffer));
        this.method = method;
    }

    public MethodPipeComponent(BufferChainLink<K> inputBuffer, BoundedBuffer<V> outputBuffer, CallableWithArguments<K, V> method) {
        super(inputBuffer.createMethodInternalInputPort(), new OutputPort<>(outputBuffer));
        this.method = method;
    }

    protected void tick() {
        try {
            K consumed = input.consume();
            V result = method.call(consumed);
            output.produce(result);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static <K, V> CallableWithArguments<K, V> toMethod(CallableWithArguments<BoundedBuffer<K>, BoundedBuffer<V>> pipe){
        return new CallableWithArguments<>() {
            SimpleBuffer<K> inputBuffer = new SimpleBuffer<>(1, "pipe to method - input");
            OutputPort<K> methodInput = inputBuffer.createOutputPort();
            InputPort<V> methodOutput = pipe.call(inputBuffer).createInputPort();

            @Override
            public V call(K input) {
                try {
                    methodInput.produce(input);
                    return methodOutput.consume();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
    }

}
