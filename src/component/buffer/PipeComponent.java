package component.buffer;

import component.buffer.BoundedBuffer;
import component.buffer.CallableWithArguments;
import component.buffer.InputPort;
import component.buffer.OutputPort;

public class PipeComponent<K, V> {
    protected final InputPort<K> input;
    protected final OutputPort<V> output;
    protected final CallableWithArguments<K, V> method;

    public PipeComponent(SimpleBuffer<K> inputBuffer, BoundedBuffer<V> outputBuffer, CallableWithArguments<K, V> method) {
        input = inputBuffer.createInputPort();
        output = new OutputPort<>(outputBuffer);
        this.method = method;
    }

    public PipeComponent(BufferChainLink<K> inputBuffer, BoundedBuffer<V> outputBuffer, CallableWithArguments<K, V> method) {
        input = inputBuffer.createMethodInternalInputPort();
        output = new OutputPort<>(outputBuffer);
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
