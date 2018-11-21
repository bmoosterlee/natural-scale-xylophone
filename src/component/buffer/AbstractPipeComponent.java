package component.buffer;

import java.util.Collection;
import java.util.Collections;

public abstract class AbstractPipeComponent<K, V> extends AbstractComponent<K, V> {
    public final InputPort<K> input;
    public final OutputPort<V> output;

    public AbstractPipeComponent(InputPort<K> input, OutputPort<V> output) {
        this.input = input;
        this.output = output;
    }

    @Override
    public Collection<BoundedBuffer<K>> getInputBuffers() {
        return Collections.singleton(input.getBuffer());
    }

    @Override
    public Collection<BoundedBuffer<V>> getOutputBuffers() {
        return Collections.singleton(output.getBuffer());
    }

    public InputPort<K> getInputPort() {
        return input;
    }

    public OutputPort<V> getOutputPort() {
        return output;
    }
}
