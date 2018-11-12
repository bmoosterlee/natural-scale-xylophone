package component.utilities;

import component.buffer.BoundedBuffer;
import component.buffer.CallableWithArgument;
import component.buffer.InputPort;

public class InputComponent<K> {
    protected final InputPort<K> input;
    protected final CallableWithArgument<K> method;

    public InputComponent(BoundedBuffer<K> inputBuffer, CallableWithArgument<K> method) {
        input = new InputPort<>(inputBuffer);
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
}
