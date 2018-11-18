package component.buffer;

import java.util.concurrent.Callable;

public class RunningOutputComponent<V> extends OutputComponent<V> {

    private final MyTickRunner tickRunner = new MyTickRunner();

    public RunningOutputComponent(SimpleBuffer<V> outputBuffer, Callable<V> method){
        super(outputBuffer, method);

        start();
    }

    private class MyTickRunner extends SimpleTickRunner {

        protected void tick() {
            RunningOutputComponent.this.tick();
        }

    }
    protected void start() {
        tickRunner.start();
    }

    public static <V> SimpleBuffer<V> buildOutputBuffer(Callable<V> method, int capacity, String name) {
        SimpleBuffer<V> outputBuffer = new SimpleBuffer<>(capacity, name);
        new RunningOutputComponent<>(outputBuffer, method);
        return outputBuffer;
    }
}
