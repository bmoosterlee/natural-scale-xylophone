package component.buffer;

public class MethodPipeComponent<K, V> extends AbstractPipeComponent<K, V> {
    protected final PipeCallable<K, V> method;

    public MethodPipeComponent(SimpleBuffer<K> inputBuffer, BoundedBuffer<V> outputBuffer, PipeCallable<K, V> method) {
        super(inputBuffer.createInputPort(), new OutputPort<>(outputBuffer));
        this.method = method;
    }

    public MethodPipeComponent(BufferChainLink<K> inputBuffer, BoundedBuffer<V> outputBuffer, PipeCallable<K, V> method) {
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

    @Override
    public Boolean isParallelisable(){
        return method.isParallelisable();
    }

    public static <K, V> PipeCallable<K, V> toMethod(PipeCallable<BoundedBuffer<K>, BoundedBuffer<V>> pipe){
        return new PipeCallable<>() {
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

            @Override
            public Boolean isParallelisable(){
                return pipe.isParallelisable();
            }
        };
    }

}
