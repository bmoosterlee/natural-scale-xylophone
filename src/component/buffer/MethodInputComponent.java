package component.buffer;

public class MethodInputComponent<K> extends AbstractInputComponent<K> {
    protected final InputCallable<K> method;
    private final Boolean parallelisability;

    public MethodInputComponent(SimpleBuffer<K> inputBuffer, InputCallable<K> method) {
        super(inputBuffer.createInputPort());
        this.method = method;
        parallelisability = method.isParallelisable();
    }

    public MethodInputComponent(BufferChainLink<K> inputBuffer, InputCallable<K> method) {
        super(inputBuffer.createMethodInternalInputPort());
        this.method = method;
        parallelisability = method.isParallelisable();
    }

    protected void tick() {
        try {
            K consumed = input.consume();
            method.call(consumed);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Boolean isParallelisable(){
        return parallelisability;
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
        };
    }
}
