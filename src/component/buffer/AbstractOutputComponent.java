package component.buffer;

import java.util.Collection;
import java.util.Collections;

public abstract class AbstractOutputComponent<V> extends AbstractComponent<Object, V> {
    protected final OutputPort<V> output;

    public AbstractOutputComponent(OutputPort<V> output) {
        this.output = output;
    }

    @Override
    public Collection<BoundedBuffer<Object>> getInputBuffers() {
        return Collections.emptyList();
    }

    @Override
    public Collection<BoundedBuffer<V>> getOutputBuffers() {
        return Collections.singleton(output.getBuffer());
    }
}
