package component.buffer;

public class TickRunningStrategy {

    public TickRunningStrategy(final AbstractComponent pipeComponent){
        this(pipeComponent, false);
    }

    public <K, V> TickRunningStrategy(final AbstractComponent<K, V> pipeComponent, boolean threadAlreadyRunning){
        if(pipeComponent.isParallelisable()){
            new TickRunnerSpawner<>(pipeComponent, threadAlreadyRunning).start();
        }
        else {
            new SimpleTickRunner(pipeComponent).start();
        }
    }

}
