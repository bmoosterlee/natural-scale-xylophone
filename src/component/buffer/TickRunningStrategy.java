package component.buffer;

import java.util.concurrent.Callable;

public class TickRunningStrategy<K, V> {

    public TickRunningStrategy(final AbstractPipeComponent<K, V> pipeComponent){
        new SimpleTickRunner() {
            @Override
            protected void tick() {
                pipeComponent.tick();
            }
        }.start();
    }

    public TickRunningStrategy(final AbstractInputComponent<K> inputComponent){
        new SimpleTickRunner() {
            @Override
            protected void tick() {
                inputComponent.tick();
            }
        }.start();
    }

    public TickRunningStrategy(final AbstractOutputComponent<V> outputComponent){

        new SimpleTickRunner() {
            protected void tick() {
                outputComponent.tick();
            }
        }.start();
    }

    public static <V> SimpleBuffer<V> buildOutputBuffer(Callable<V> method, int capacity, String name) {
        SimpleBuffer<V> outputBuffer = new SimpleBuffer<>(capacity, name);
        new TickRunningStrategy<>(new MethodOutputComponent<>(outputBuffer, method));
        return outputBuffer;
    }

}
