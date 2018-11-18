package component.buffer;

import java.util.concurrent.Callable;

public class RunningOutputComponent<V> {

    private final OutputComponent<V> outputComponent;

    public RunningOutputComponent(final OutputComponent<V> outputComponent){
        this.outputComponent = outputComponent;

        new SimpleTickRunner() {
            protected void tick() {
                RunningOutputComponent.this.tick();
            }
        }.start();
    }

    protected void tick() {
        outputComponent.tick();
    }

    public static <V> SimpleBuffer<V> buildOutputBuffer(Callable<V> method, int capacity, String name) {
        SimpleBuffer<V> outputBuffer = new SimpleBuffer<>(capacity, name);
        new RunningOutputComponent<>(new OutputComponent<>(outputBuffer, method));
        return outputBuffer;
    }
}
