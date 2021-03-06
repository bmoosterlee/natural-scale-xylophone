package component.buffer;

public class MethodOutputComponent<V> extends AbstractOutputComponent<V, SimplePacket<V>> {
    protected final OutputCallable<V> method;

    public MethodOutputComponent(BoundedBuffer<V, SimplePacket<V>> outputBuffer, OutputCallable<V> method) {
        super(new OutputPort<>(outputBuffer));
        this.method = method;
    }

    public void tick() {
        try {
            V result = method.call();
            output.produce(new SimplePacket<>(result));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
