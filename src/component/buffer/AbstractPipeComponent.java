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

    protected abstract void tick();

    @Override
    public Collection<InputPort<K>> getInputPorts() {
        return Collections.singleton(input);
    }

    @Override
    public Collection<OutputPort<V>> getOutputPorts() {
        return Collections.singleton(output);
    }
}
