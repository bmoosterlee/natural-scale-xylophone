package component.buffer;

import java.util.Collection;

public abstract class AbstractComponent<K, V, A extends Packet<K>, B extends Packet<V>> {

    protected abstract Collection<BoundedBuffer<K, A>> getInputBuffers();

    protected abstract Collection<BoundedBuffer<V, B>> getOutputBuffers();

    protected abstract void tick();

}
