package component.buffer;

import java.util.Collection;
import java.util.Collections;

public abstract class AbstractOutputComponent<V, B extends Packet<V>> extends AbstractComponent<Void, V, Packet<Void>, B> {
    protected final OutputPort<V, B> output;

    public AbstractOutputComponent(OutputPort<V, B> output) {
        this.output = output;
    }

    @Override
    public Collection<BoundedBuffer<Void, Packet<Void>>> getInputBuffers() {
        return Collections.emptyList();
    }

    @Override
    public Collection<BoundedBuffer<V, B>> getOutputBuffers() {
        return Collections.singleton(output.getBuffer());
    }
}
