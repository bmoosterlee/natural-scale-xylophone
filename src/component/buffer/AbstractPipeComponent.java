package component.buffer;

public abstract class AbstractPipeComponent<K, V> {
    protected final InputPort<K> input;
    protected final OutputPort<V> output;

    public AbstractPipeComponent(InputPort<K> input, OutputPort<V> output) {
        this.input = input;
        this.output = output;
    }

    protected abstract void tick();
}
