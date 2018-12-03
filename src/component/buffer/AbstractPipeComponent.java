package component.buffer;

import java.util.Collection;
import java.util.Collections;

public abstract class AbstractPipeComponent<K, V, A extends Packet<K>, B extends Packet<V>> extends AbstractComponent<K, V, A, B> {
    public final InputPort<K, A> input;
    public final OutputPort<V, B> output;

    public AbstractPipeComponent(InputPort<K, A> input, OutputPort<V, B> output) {
        this.input = input;
        this.output = output;
    }

    @Override
    public Collection<BoundedBuffer<K, A>> getInputBuffers() {
        return Collections.singleton(input.getBuffer());
    }

    @Override
    public Collection<BoundedBuffer<V, B>> getOutputBuffers() {
        return Collections.singleton(output.getBuffer());
    }

    public InputPort<K, A> getInputPort() {
        return input;
    }

    public OutputPort<V, B> getOutputPort() {
        return output;
    }
}
