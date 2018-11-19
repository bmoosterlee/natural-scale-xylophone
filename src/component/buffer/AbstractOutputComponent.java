package component.buffer;

public abstract class AbstractOutputComponent<V> {
    protected final OutputPort<V> output;

    public AbstractOutputComponent(OutputPort<V> output) {
        this.output = output;
    }

    protected abstract void tick();
}
