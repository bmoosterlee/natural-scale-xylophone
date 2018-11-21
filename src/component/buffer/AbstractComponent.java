package component.buffer;

import java.util.Collection;

public abstract class AbstractComponent<K, V> {

    protected abstract Collection<InputPort<K>> getInputPorts();

    protected abstract Collection<OutputPort<V>> getOutputPorts();

    protected abstract void tick();

    public Boolean isParallelisable(){
        return true;
    }
}
