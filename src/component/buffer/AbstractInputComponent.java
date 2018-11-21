package component.buffer;

import java.util.Collection;
import java.util.Collections;

public abstract class AbstractInputComponent<K> extends AbstractComponent<K, Object> {
    protected final InputPort<K> input;

    public AbstractInputComponent(InputPort<K> input) {
        this.input = input;
    }

    @Override
    public Collection<BoundedBuffer<K>> getInputBuffers() {
        return Collections.singleton(input.getBuffer());
    }

    @Override
    public Collection<BoundedBuffer<Object>> getOutputBuffers() {
        return Collections.emptyList();
    }
}
