package component.buffer;

import component.buffer.OutputPort;
import component.buffer.SimpleBuffer;

import java.util.concurrent.Callable;

public class OutputComponent<V> {
    protected final OutputPort<V> output;
    protected final Callable<V> method;

    public OutputComponent(SimpleBuffer<V> outputBuffer, Callable<V> method) {
        output = new OutputPort<>(outputBuffer);
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
