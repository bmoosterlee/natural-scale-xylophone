package component.buffer;

public abstract class AbstractInputComponent<K> {
    protected final InputPort<K> input;

    public AbstractInputComponent(InputPort<K> input) {
        this.input = input;
    }

    protected abstract void tick();
}
