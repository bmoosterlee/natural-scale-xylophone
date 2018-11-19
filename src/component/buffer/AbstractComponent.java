package component.buffer;

import java.util.Collection;

public abstract class AbstractComponent<K, V> {

    abstract Collection<InputPort<K>> getInputPorts();

    abstract Collection<OutputPort<V>> getOutputPorts();

}
