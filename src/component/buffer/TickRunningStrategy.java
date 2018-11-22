package component.buffer;

import java.util.concurrent.Callable;

public class TickRunningStrategy {

    public TickRunningStrategy(final AbstractComponent pipeComponent){
        this(pipeComponent, false);
    }

    public TickRunningStrategy(final AbstractComponent pipeComponent, boolean threadAlreadyRunning){
        if(pipeComponent.isParallelisable()){
            new TickRunnerSpawner(pipeComponent, threadAlreadyRunning).start();
        }
        else {
            new SimpleTickRunner(pipeComponent).start();
        }
    }

    public static <V> SimpleBuffer<V> buildOutputBuffer(Callable<V> method, int capacity, String name) {
        SimpleBuffer<V> outputBuffer = new SimpleBuffer<>(capacity, name);
        new TickRunningStrategy(new MethodOutputComponent<>(outputBuffer, method));
        return outputBuffer;
    }

}
