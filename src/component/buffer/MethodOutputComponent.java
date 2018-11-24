package component.buffer;

import main.OutputCallable;

public class MethodOutputComponent<V> extends AbstractOutputComponent<V> {
    protected final OutputCallable<V> method;

    public MethodOutputComponent(SimpleBuffer<V> outputBuffer, OutputCallable<V> method) {
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

}
