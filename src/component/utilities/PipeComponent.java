package component.utilities;

import component.buffer.BoundedBuffer;
import component.buffer.CallableWithArguments;
import component.buffer.InputPort;
import component.buffer.OutputPort;

public class PipeComponent<K, V> {
    protected final InputPort<K> input;
    protected final OutputPort<V> output;
    protected final CallableWithArguments<K, V> method;

    public PipeComponent(BoundedBuffer<K> inputBuffer, BoundedBuffer<V> outputBuffer, CallableWithArguments<K, V> method) {
        input = inputBuffer.createInputPort();
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

}
