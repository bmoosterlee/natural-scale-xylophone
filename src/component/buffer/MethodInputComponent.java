package component.buffer;

public class MethodInputComponent<K> extends AbstractInputComponent<K> {
    protected final CallableWithArgument<K> method;

    public MethodInputComponent(SimpleBuffer<K> inputBuffer, CallableWithArgument<K> method) {
        super(inputBuffer.createInputPort());
        this.method = method;
    }

    public MethodInputComponent(BufferChainLink<K> inputBuffer, CallableWithArgument<K> method) {
        super(inputBuffer.createMethodInternalInputPort());
        this.method = method;
    }

    protected void tick() {
        try {
            K consumed = input.consume();
            method.call(consumed);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static <K> CallableWithArgument<K> toMethod(CallableWithArgument<BoundedBuffer<K>> pipe){
        return new CallableWithArgument<>() {
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
