package component.buffer;

import java.util.concurrent.Callable;

public class MethodOutputComponent<V> extends AbstractOutputComponent<V> {
    protected final Callable<V> method;

    public MethodOutputComponent(SimpleBuffer<V> outputBuffer, Callable<V> method) {
        super(new OutputPort<>(outputBuffer));
        this.method = method;
    }

    public void tick() {
        try {
            V result = method.call();
            output.produce(result);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
