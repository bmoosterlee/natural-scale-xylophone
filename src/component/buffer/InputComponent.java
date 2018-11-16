package component.buffer;

import component.buffer.BoundedBuffer;
import component.buffer.CallableWithArgument;
import component.buffer.InputPort;

public class InputComponent<K> {
    protected final InputPort<K> input;
    protected final CallableWithArgument<K> method;

    public InputComponent(SimpleBuffer<K> inputBuffer, CallableWithArgument<K> method) {
        input = inputBuffer.createInputPort();
        this.method = method;
    }

    public InputComponent(BufferChainLink<K> inputBuffer, CallableWithArgument<K> method) {
        input = inputBuffer.createMethodInternalInputPort();
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
