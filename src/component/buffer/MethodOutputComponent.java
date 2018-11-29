package component.buffer;

public class MethodOutputComponent<V> extends AbstractOutputComponent<V> {
    protected final OutputCallable<V> method;

    public MethodOutputComponent(BoundedBuffer<V> outputBuffer, OutputCallable<V> method) {
        super(new OutputPort<>(outputBuffer));
        this.method = method;
    }

    public void tick() {
        try {
            V result = method.call();
            output.produce(result);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Boolean isParallelisable(){
        return method.isParallelisable();
    }

}
