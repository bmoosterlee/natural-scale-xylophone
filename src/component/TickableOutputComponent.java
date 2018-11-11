package component;

import java.util.concurrent.Callable;

public class TickableOutputComponent<V> extends Tickable {

    protected final OutputPort<V> output;
    private final Callable<V> method;

    public TickableOutputComponent(BoundedBuffer<V> outputBuffer, Callable<V> method){
        this.method = method;

        output = new OutputPort<>(outputBuffer);

        start();
    }

    @Override
    protected void tick() {
        try {
            V result = method.call();
            output.produce(result);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static <V> BoundedBuffer<V> buildOutputBuffer(Callable<V> method, int capacity, String name) {
        BoundedBuffer<V> outputBuffer = new BoundedBuffer<>(capacity, name);
        new TickableOutputComponent<>(outputBuffer, method);
        return outputBuffer;
    }

}
