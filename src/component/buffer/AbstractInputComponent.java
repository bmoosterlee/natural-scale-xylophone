package component.buffer;

import java.util.Collection;
import java.util.Collections;

public abstract class AbstractInputComponent<K, T extends Packet<K>> extends AbstractComponent<K, Void, T, Packet<Void>> {
    protected final InputPort<K, T> input;

    public AbstractInputComponent(InputPort<K, T> input) {
        this.input = input;
    }

    @Override
    public Collection<BoundedBuffer<K, T>> getInputBuffers() {
        return Collections.singleton(input.getBuffer());
    }

    @Override
    public Collection<BoundedBuffer<Void, Packet<Void>>> getOutputBuffers() {
        return Collections.emptyList();
    }
}
