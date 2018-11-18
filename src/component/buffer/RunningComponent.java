package component.buffer;

import java.util.concurrent.Callable;

public class RunningComponent<K, V> {

    public RunningComponent(final PipeComponent<K, V> pipeComponent){
        new SimpleTickRunner() {
            @Override
            protected void tick() {
                pipeComponent.tick();
            }
        }.start();
    }

    public RunningComponent(final InputComponent<K> inputComponent){
        new SimpleTickRunner() {
            @Override
            protected void tick() {
                inputComponent.tick();
            }
        }.start();
    }

    public RunningComponent(final OutputComponent<V> outputComponent){

        new SimpleTickRunner() {
            protected void tick() {
                outputComponent.tick();
            }
        }.start();
    }

    public static <V> SimpleBuffer<V> buildOutputBuffer(Callable<V> method, int capacity, String name) {
        SimpleBuffer<V> outputBuffer = new SimpleBuffer<>(capacity, name);
        new RunningComponent<>(new OutputComponent<>(outputBuffer, method));
        return outputBuffer;
    }

}
