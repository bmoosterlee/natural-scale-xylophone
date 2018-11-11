package component;

import java.util.concurrent.Callable;

public class TickableOutputComponent<V> extends Tickable {

    protected final OutputPort<V> output;
    private final Callable<V> method;

    public TickableOutputComponent(SimpleBuffer<V> outputBuffer, Callable<V> method){
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

    public static <V> SimpleBuffer<V> buildOutputBuffer(Callable<V> method, int capacity, String name) {
        SimpleBuffer<V> outputBuffer = new SimpleBuffer<>(capacity, name);
        new TickableOutputComponent<>(outputBuffer, method);
        return outputBuffer;
    }

}
