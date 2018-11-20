package component.buffer;

import java.util.Collection;
import java.util.Collections;

public abstract class AbstractInputComponent<K> extends AbstractComponent<K, Object> {
    protected final InputPort<K> input;

    public AbstractInputComponent(InputPort<K> input) {
        this.input = input;
    }

    @Override
    public Collection<InputPort<K>> getInputPorts() {
        return Collections.singleton(input);
    }

    @Override
    public Collection<OutputPort<Object>> getOutputPorts() {
        return Collections.emptyList();
    }
}
