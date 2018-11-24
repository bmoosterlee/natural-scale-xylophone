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

    public static <V> SimpleBuffer<V> buildOutputBuffer(Callable<V> method, int capacity, String name) {
        SimpleBuffer<V> outputBuffer = new SimpleBuffer<>(capacity, name);
        new TickRunningStrategy(new MethodOutputComponent<>(outputBuffer, method));
        return outputBuffer;
    }
}
