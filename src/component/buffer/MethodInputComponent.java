package component.buffer;

public class MethodInputComponent<K> extends AbstractInputComponent<K> {
    protected final InputCallable<K> method;

    public MethodInputComponent(SimpleBuffer<K> inputBuffer, InputCallable<K> method) {
        super(inputBuffer.createInputPort());
        this.method = method;
    }

    public MethodInputComponent(BufferChainLink<K> inputBuffer, InputCallable<K> method) {
        super(inputBuffer.createMethodInternalInputPort());
        this.method = method;
    }

    protected void tick() {
        K consumed = consume();
        process(consumed);
    }

    protected void tryTick() {
        K consumed = tryConsume();
        process(consumed);
    }

    private K consume() {
        try {
            return input.consume();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    private K tryConsume() {
        return input.tryConsume();
    }

    private void process(K consumed) {
        method.call(consumed);
    }

    @Override
    public Boolean isParallelisable(){
        return method.isParallelisable();
    }

    public static <K> InputCallable<K> toMethod(InputCallable<BoundedBuffer<K>> pipe){
        return new InputCallable<>() {
            SimpleBuffer<K> inputBuffer = new SimpleBuffer<>(1, "pipe to method - input");
            OutputPort<K> methodInput = inputBuffer.createOutputPort();
            {
                pipe.call(inputBuffer);
            }

            @Override
            public void call(K input) {
                try {
                    methodInput.produce(input);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public Boolean isParallelisable(){
                return pipe.isParallelisable();
            }
        };
    }
}
