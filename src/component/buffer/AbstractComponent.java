package component.buffer;

import java.util.Collection;

public abstract class AbstractComponent<K, V> {

    protected abstract Collection<BoundedBuffer<K>> getInputBuffers();

    protected abstract Collection<BoundedBuffer<V>> getOutputBuffers();

    protected abstract void tick();

    public Boolean isParallelisable(){
        return true;
    }
}
