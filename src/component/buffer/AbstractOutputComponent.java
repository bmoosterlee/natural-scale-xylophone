package component.buffer;

import java.util.Collection;
import java.util.Collections;

public abstract class AbstractOutputComponent<V> extends AbstractComponent<Object, V> {
    protected final OutputPort<V> output;

    public AbstractOutputComponent(OutputPort<V> output) {
        this.output = output;
    }

    protected abstract void tick();

    @Override
    public Collection<InputPort<Object>> getInputPorts() {
        return Collections.emptyList();
    }

    @Override
    public Collection<OutputPort<V>> getOutputPorts() {
        return Collections.singleton(output);
    }
}
